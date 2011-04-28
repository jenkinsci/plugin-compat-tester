package org.jenkins.tools.test;

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenRequest;
import hudson.model.UpdateSite;
import hudson.model.UpdateSite.Plugin;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.exception.PomTransformationException;
import org.jenkins.tools.test.model.*;
import org.jenkins.tools.test.model.comparators.MavenCoordinatesComparator;
import org.springframework.core.io.ClassPathResource;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

public class PluginCompatTester {

	private PluginCompatTesterConfig config;
	
	public PluginCompatTester(PluginCompatTesterConfig config){
        this.config = config;
	}

    public SortedSet<MavenCoordinates> generateCoreCoordinatesToTest(UpdateSite.Data data, PluginCompatReport previousReport){
        SortedSet<MavenCoordinates> coreCoordinatesToTest = null;
        // If parent GroupId/Artifact are not null, this will be fast : we will only test
        // against 1 core coordinate
        if(config.getParentGroupId() != null && config.getParentArtifactId() != null){
            coreCoordinatesToTest = new TreeSet<MavenCoordinates>(new MavenCoordinatesComparator());

            // If coreVersion is not provided in PluginCompatTesterConfig, let's use latest core
            // version used in update center
            String coreVersion = config.getParentVersion()==null?data.core.version:config.getParentVersion();

            MavenCoordinates coreArtifact = new MavenCoordinates(config.getParentGroupId(), config.getParentArtifactId(), coreVersion);
            coreCoordinatesToTest.add(coreArtifact);
        // If parent groupId/artifactId are null, we'll test against every already recorded
        // cores
        } else if(config.getParentGroupId() == null && config.getParentArtifactId() == null){
            coreCoordinatesToTest = previousReport.getTestedCoreCoordinates();
        } else {
            throw new IllegalStateException("config.parentGroupId and config.parentArtifactId should either be both null or both filled\n" +
                    "config.parentGroupId="+String.valueOf(config.getParentGroupId())+", config.parentArtifactId="+String.valueOf(config.getParentArtifactId()));
        }

        return coreCoordinatesToTest;
    }

	public PluginCompatReport testPlugins()
        throws PlexusContainerException, IOException, MavenEmbedderException
    {
        // Providing XSL Stylesheet along xml report file
        if(config.reportFile != null){
            if(config.isProvideXslReport()){
                File xslFilePath = PluginCompatReport.getXslFilepath(config.reportFile);
                FileUtils.copyStreamToFile(new RawInputStreamFacade(getXslTransformerResource().getInputStream()), xslFilePath);
            }
        }

        UpdateSite.Data data = extractUpdateCenterData();
        PluginCompatReport report = PluginCompatReport.fromXml(config.reportFile);

        SortedSet<MavenCoordinates> testedCores = generateCoreCoordinatesToTest(data, report);

        //here we don't care about paths for build the embedder
        MavenRequest mavenRequest = buildMavenRequest( null, null );
        MavenEmbedder embedder = new MavenEmbedder(Thread.currentThread().getContextClassLoader(), mavenRequest);

		SCMManagerFactory.getInstance().start();
        for(MavenCoordinates coreCoordinates : testedCores){
            System.out.println("Starting plugin tests on core coordinates : "+coreCoordinates.toString());
            for(Entry<String, Plugin> pluginEntry : data.plugins.entrySet()){
                Plugin plugin = pluginEntry.getValue();
                if(config.getIncludePlugins()==null || config.getIncludePlugins().contains(plugin.name.toLowerCase())){
                    PluginInfos pluginInfos = new PluginInfos(plugin);

                    if(config.getExcludePlugins()!=null && config.getExcludePlugins().contains(plugin.name.toLowerCase())){
                        System.out.println("Plugin "+plugin.name+" is in excluded plugins => test skipped !");
                        continue;
                    }

                    if(!config.isSkipTestCache() && report.isCompatTestResultAlreadyInCache(pluginInfos, coreCoordinates, config.getTestCacheTimeout())){
                        System.out.println("Cache activated for plugin "+pluginInfos.pluginName+" => test skipped !");
                        continue; // Don't do anything : we are in the cached interval ! :-)
                    }

                    boolean compilationOk = false;
                    boolean testsOk = false;
                    String errorMessage = null;

                    TestStatus status;
                    List<String> warningMessages = new ArrayList<String>();
                    try {
                        TestExecutionResult result = testPluginAgainst(coreCoordinates, plugin, embedder);
                        // If no PomExecutionException, everything went well...
                        status = TestStatus.SUCCESS;
                        warningMessages.addAll(result.pomWarningMessages);
                    } catch (PomExecutionException e) {
                        if(!e.succeededPluginArtifactIds.contains("maven-compiler-plugin")){
                            status = TestStatus.COMPILATION_ERROR;
                        } else if(!e.succeededPluginArtifactIds.contains("maven-surefire-plugin")){
                            status = TestStatus.TEST_FAILURES;
                        } else { // Can this really happen ???
                            status = TestStatus.SUCCESS;
                        }
                        errorMessage = e.getErrorMessage();
                        warningMessages.addAll(e.getPomWarningMessages());
                    } catch (Error e){
                        // Rethrow the error ... something is getting wrong !
                        throw e;
                    } catch (Throwable t){
                        status = TestStatus.INTERNAL_ERROR;
                        errorMessage = t.getMessage();
                    }

                    PluginCompatResult result = new PluginCompatResult(coreCoordinates, status, errorMessage, warningMessages);
                    report.add(pluginInfos, result);

                    if(config.reportFile != null){
                        if(!config.reportFile.exists()){
                            FileUtils.fileWrite(config.reportFile.getAbsolutePath(), "");
                        }
                        report.save(config.reportFile);
                    }
                } else {
                    System.out.println("Plugin "+plugin.name+" not in included plugins => test skipped !");
                }
            }
        }

        // Generating HTML report if needed
        if(config.reportFile != null){
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
	
	public TestExecutionResult testPluginAgainst(MavenCoordinates coreCoordinates, Plugin plugin, MavenEmbedder embedder)
        throws PluginSourcesUnavailableException, PomTransformationException, PomExecutionException, IOException
    {
		File pluginCheckoutDir = new File(config.workDirectory.getAbsolutePath()+"/"+plugin.name+"/");
		pluginCheckoutDir.mkdir();
		System.out.println("Created plugin checkout dir : "+pluginCheckoutDir.getAbsolutePath());
		
		PluginRemoting remote = new PluginRemoting(plugin.url);
		PomData pomData = remote.retrievePomData();
		
		try {
            System.out.println("Checkouting from scm connection URL : "+pomData.getConnectionUrl());
			ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
			ScmRepository repository = scmManager.makeScmRepository(pomData.getConnectionUrl());
            System.out.println("Checkouting from scm connection URL : "+pomData.getConnectionUrl());
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
		pom.transformPom(coreCoordinates);



		// Calling maven
        try {

            MavenRequest mavenRequest = buildMavenRequest( pluginCheckoutDir.getAbsolutePath(),
                                                           config.getM2SettingsFile() == null
                                                               ? null
                                                               : config.getM2SettingsFile().getAbsolutePath() );
            mavenRequest.setGoals(Arrays.asList( "clean","install"));
            mavenRequest.setPom(pluginCheckoutDir.getAbsolutePath()+"/pom.xml");
            MavenExecutionResult mavenResult = pom.executeGoals(embedder, mavenRequest);

            return new TestExecutionResult(mavenResult, pomData.getWarningMessages());
        }catch(PomExecutionException e){
            e.setPomWarningMessages(pomData.getWarningMessages());
            throw e;
        }
	}

    private MavenRequest buildMavenRequest(String rootDir,String settingsPath)
        throws IOException
    {

        MavenRequest mavenRequest = new MavenRequest();

        mavenRequest.setBaseDirectory(rootDir);

        mavenRequest.setUserSettingsFile(settingsPath);

        // TODO REMOVE
        mavenRequest.getUserProperties().put( "failIfNoTests", "false" );
        mavenRequest.getUserProperties().put( "argLine", "-XX:MaxPermSize=128m" );

        String mavenPropertiesFilePath = this.config.getMavenPropertiesFile();

        if ( StringUtils.isNotBlank( mavenPropertiesFilePath )) {
            File file = new File (mavenPropertiesFilePath);
            if (file.exists()) {
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream( file );
                    Properties properties = new Properties(  );
                    properties.load( fileInputStream  );
                    mavenRequest.getUserProperties().putAll( properties );
                } finally {
                    IOUtils.closeQuietly( fileInputStream );
                }
            } else {
                System.out.println("File " + mavenPropertiesFilePath + " not exists" );
            }

        }

        return mavenRequest;

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
