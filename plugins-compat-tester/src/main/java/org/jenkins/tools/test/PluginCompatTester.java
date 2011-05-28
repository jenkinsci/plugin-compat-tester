/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Erik Ramfelt, Koichi Fujikawa, Red Hat, Inc., Seiji Sogabe,
 * Stephen Connolly, Tom Huybrechts, Yahoo! Inc., Alan Harder, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkins.tools.test;

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenRequest;
import hudson.model.UpdateSite;
import hudson.model.UpdateSite.Plugin;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.exception.PomTransformationException;
import org.jenkins.tools.test.logging.SystemIOLoggerFilter;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.MavenPom;
import org.jenkins.tools.test.model.PluginCompatReport;
import org.jenkins.tools.test.model.PluginCompatResult;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PluginInfos;
import org.jenkins.tools.test.model.PluginRemoting;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.TestExecutionResult;
import org.jenkins.tools.test.model.TestStatus;
import org.springframework.core.io.ClassPathResource;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Frontend for plugin compatibility tests
 * @author Frederic Camblor, Olivier Lamy
 */
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
            coreCoordinatesToTest = new TreeSet<MavenCoordinates>();

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

                    if(!config.isSkipTestCache() && report.isCompatTestResultAlreadyInCache(pluginInfos, coreCoordinates, config.getTestCacheTimeout(), config.getCacheThresholStatus())){
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

    private static String createBuildLogFilePathFor(String pluginName, String pluginVersion, MavenCoordinates coreCoords){
        return String.format("logs/%s/v%s_against_%s_%s_%s.log", pluginName, pluginVersion, coreCoords.groupId, coreCoords.artifactId, coreCoords.version);
    }
	
	public TestExecutionResult testPluginAgainst(MavenCoordinates coreCoordinates, Plugin plugin, MavenEmbedder embedder)
        throws PluginSourcesUnavailableException, PomTransformationException, PomExecutionException, IOException
    {
        System.out.println(String.format("%n%n%n%n%n"));
        System.out.println(String.format("#############################################"));
        System.out.println(String.format("#############################################"));
        System.out.println(String.format("##%n## Starting to test plugin %s v%s%n## against %s%n##", plugin.name, plugin.version, coreCoordinates));
        System.out.println(String.format("#############################################"));
        System.out.println(String.format("#############################################"));
        System.out.println(String.format("%n%n%n%n%n"));

		File pluginCheckoutDir = new File(config.workDirectory.getAbsolutePath()+"/"+plugin.name+"/");
        if(pluginCheckoutDir.exists()){
            System.out.println("Deleting working directory "+pluginCheckoutDir.getAbsolutePath());
            FileUtils.deleteDirectory(pluginCheckoutDir);
        }
		pluginCheckoutDir.mkdir();
		System.out.println("Created plugin checkout dir : "+pluginCheckoutDir.getAbsolutePath());
		
		PluginRemoting remote = new PluginRemoting(plugin.url);
		PomData pomData = remote.retrievePomData();
		
		try {
            System.out.println("Checkouting from scm connection URL : "+pomData.getConnectionUrl()+" ("+plugin.name+"-"+plugin.version+")");
			ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
			ScmRepository repository = scmManager.makeScmRepository(pomData.getConnectionUrl());
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



        final List<String> succeededPlugins = new ArrayList<String>();
		// Calling maven
        try {

            MavenRequest mavenRequest = buildMavenRequest( pluginCheckoutDir.getAbsolutePath(),
                                                           config.getM2SettingsFile() == null
                                                               ? null
                                                               : config.getM2SettingsFile().getAbsolutePath() );
            mavenRequest.setGoals(Arrays.asList( "clean","install"));
            mavenRequest.setPom(pluginCheckoutDir.getAbsolutePath()+"/pom.xml");
            AbstractExecutionListener mavenListener = new AbstractExecutionListener(){
                public void mojoSucceeded(ExecutionEvent event){
                     succeededPlugins.add(event.getMojoExecution().getArtifactId());
                }
            };
            mavenRequest.setExecutionListener(mavenListener);

            File buildLogFile = new File(config.reportFile.getParentFile().getAbsolutePath()
                    +"/"+createBuildLogFilePathFor(plugin.name, plugin.version, coreCoordinates));
            FileUtils.forceMkdir(buildLogFile.getParentFile()); // Creating log directory
            FileUtils.fileWrite(buildLogFile.getAbsolutePath(), ""); // Creating log file

            mavenRequest.setLoggingLevel(Logger.LEVEL_INFO);

            final PrintStream originalOut = System.out;
            final PrintStream originalErr = System.err;
            SystemIOLoggerFilter loggerFilter = new SystemIOLoggerFilter(buildLogFile);

            // Since here, we are replacing System.out & System.err by
            // wrappers logging things in the build log file
            // We can't do this by using maven embedder's logger (or plexus logger)
            // since :
            // - It would imply to Instantiate a new MavenEmbedder for every test (which have a performance/memory cost !)
            // - Plus it looks like there are lots of System.out/err.println() in maven
            // plugin (instead of using maven logger)
            System.setOut(new SystemIOLoggerFilter.SystemIOWrapper(loggerFilter, originalOut));
            System.setErr(new SystemIOLoggerFilter.SystemIOWrapper(loggerFilter, originalErr));

            try {
                MavenExecutionResult mavenResult = pom.executeGoals(embedder, mavenRequest);
                return new TestExecutionResult(mavenResult, pomData.getWarningMessages());
            }finally{
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
        }catch(PomExecutionException e){
            throw new PomExecutionException(e, succeededPlugins, pomData.getWarningMessages());
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
