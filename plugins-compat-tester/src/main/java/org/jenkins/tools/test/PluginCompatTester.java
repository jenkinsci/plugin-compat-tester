package org.jenkins.tools.test;

import hudson.model.UpdateSite;
import hudson.model.UpdateSite.Plugin;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.codehaus.groovy.tools.groovydoc.ClasspathResourceManager;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.exception.PomTransformationException;
import org.jenkins.tools.test.model.*;
import org.springframework.core.io.ClassPathResource;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.Map.Entry;

public class PluginCompatTester {

	private PluginCompatTesterConfig config;
	
	public PluginCompatTester(PluginCompatTesterConfig config){
        this.config = config;
	}

	public PluginCompatReport testPlugins() throws PlexusContainerException, IOException {
        UpdateSite.Data data = extractUpdateCenterData();

        // If coreVersion is not provided in PluginCompatTesterConfig, let's use latest core
        // version used in update center
        String coreVersion = config.getParentVersion()==null?data.core.version:config.getParentVersion();
        
		SCMManagerFactory.getInstance().start();

        MavenCoordinates coreArtifact = new MavenCoordinates(config.parentGroupId, config.parentArtifactId, coreVersion);

        PluginCompatReport report = PluginCompatReport.fromXml(config.reportFile);

        for(Entry<String, Plugin> pluginEntry : data.plugins.entrySet()){
            Plugin plugin = pluginEntry.getValue();
            if(config.getPluginsList()==null || config.getPluginsList().contains(plugin.name.toLowerCase())){
                PluginInfos pluginInfos = new PluginInfos(plugin);

                if(!config.isSkipTestCache() && report.isCompatTestResultAlreadyInCache(pluginInfos, coreArtifact, config.getTestCacheTimeout())){
                    System.out.println("Cache activated for plugin "+pluginInfos.pluginName+" : test will be skipped !");
                    continue; // Don't do anything : we are in the cached interval ! :-)
                }

                boolean compilationOk = false;
                boolean testsOk = false;
                String errorMessage = null;

                try {
                    MavenExecutionResult result = testPluginAgainst(coreVersion, plugin);
                    // If no PomExecutionException, everything went well...
                    compilationOk = true;
                    testsOk = true;
                } catch (PomExecutionException e) {
                    compilationOk = e.succeededPluginArtifactIds.contains("maven-compiler-plugin");
                    testsOk = e.succeededPluginArtifactIds.contains("maven-surefire-plugin");
                    errorMessage = e.getErrorMessage();
                } catch (Throwable t){
                    errorMessage = t.getMessage();
                }

                PluginCompatResult result = new PluginCompatResult(coreArtifact, compilationOk, testsOk, errorMessage);
                report.add(pluginInfos, result);

                if(config.reportFile != null){
                    if(!config.reportFile.exists()){
                        FileUtils.fileWrite(config.reportFile.getAbsolutePath(), "");
                    }
                    report.save(config.reportFile);
                }
            } else {
                System.out.println("Plugin "+plugin.name+" not in provided pluginsList => test skipped !");
            }
        }

        if(config.reportFile != null){
            if(config.isProvideXslReport()){
                File xslFilePath = PluginCompatReport.getXslFilepath(config.reportFile);
                FileUtils.copyStreamToFile(new RawInputStreamFacade(getXslTransformerResource().getInputStream()), xslFilePath);
            }

            if(config.isGenerateHtmlReport()){
                generateHtmlReportFile();
            }
        }

        return report;
	}

    public void generateHtmlReportFile() throws IOException {
        Source xmlSource = new StreamSource(config.reportFile);
        Source xsltSource = new StreamSource(getXslTransformerResource().getInputStream());
        Result result = new StreamResult(PluginCompatReport.getHtmlFilepath(config.reportFile));

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = factory.newTransformer(xsltSource);
            transformer.transform(xmlSource, result);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private static ClassPathResource getXslTransformerResource(){
        return new ClassPathResource("resultToReport.xsl");
    }
	
	public MavenExecutionResult testPluginAgainst(String coreVersion, Plugin plugin) throws PluginSourcesUnavailableException, PomTransformationException, PomExecutionException {
		File pluginCheckoutDir = new File(config.workDirectory.getAbsolutePath()+"/"+plugin.name+"/");
		pluginCheckoutDir.mkdir();
		System.out.println("Created plugin checkout dir : "+pluginCheckoutDir.getAbsolutePath());
		
		PluginRemoting remote = new PluginRemoting(plugin.url);
		PomData pomData = remote.retrievePomData();
		
		try {
			ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
			ScmRepository repository = scmManager.makeScmRepository(pomData.connectionUrl);
			CheckOutScmResult result = scmManager.checkOut(repository, new ScmFileSet(pluginCheckoutDir), new ScmTag(plugin.name+"-"+plugin.version));
			
			if(!result.isSuccess()){
				throw new RuntimeException(result.getProviderMessage() + "||" + result.getCommandOutput());
			}
		} catch (ComponentLookupException e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem while creating ScmManager !", e);
		} catch (ScmException e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem while checkouting plugin sources !", e);
		}
		
		MavenPom pom = new MavenPom(pluginCheckoutDir, config.getM2SettingsFile());
        // If core version has not been set in GAV : use the latest available
        // in update center
		pom.transformPom(config.parentGroupId, config.parentArtifactId,
                config.getParentVersion()==null?coreVersion:config.getParentVersion());
		
		// Calling maven
        return pom.executeGoals(Arrays.asList("test"));
	}
	
	protected UpdateSite.Data extractUpdateCenterData(){
		URL url = null;
		String jsonp = null;
		try {
	        url = new URL(config.updateCenterUrl);
	        jsonp = IOUtils.toString(url.openStream());
		}catch(IOException e){
			throw new RuntimeException("Invalid update center url : "+config.updateCenterUrl, e);
		}
		
        String json = jsonp.substring(jsonp.indexOf('(')+1,jsonp.lastIndexOf(')'));

        UpdateSite us = new UpdateSite("default", url.toExternalForm());
        UpdateSite.Data data = null;
        try {
	        Constructor<UpdateSite.Data> dataConstructor = UpdateSite.Data.class.getDeclaredConstructor(UpdateSite.class, JSONObject.class);
	        dataConstructor.setAccessible(true);
	        data = dataConstructor.newInstance(us, JSONObject.fromObject(json));
        }catch(Exception e){
        	throw new RuntimeException("UpdateSite.Data instanciation problems", e);
        }
		
        return data;
	}
}
