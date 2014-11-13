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

import hudson.Functions;
import hudson.maven.MavenEmbedderException;
import hudson.model.UpdateSite;
import hudson.model.UpdateSite.Plugin;
import hudson.util.VersionNumber;
import java.io.BufferedReader;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.InternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;

/**
 * Frontend for plugin compatibility tests
 * @author Frederic Camblor, Olivier Lamy
 */
public class PluginCompatTester {

    private static final String DEFAULT_SOURCE_ID = "default";

	private PluginCompatTesterConfig config;
    private final MavenRunner runner;
	
	public PluginCompatTester(PluginCompatTesterConfig config){
        this.config = config;
        runner = config.getExternalMaven() == null ? new InternalMavenRunner() : new ExternalMavenRunner(config.getExternalMaven());
	}

    private SortedSet<MavenCoordinates> generateCoreCoordinatesToTest(UpdateSite.Data data, PluginCompatReport previousReport){
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

        DataImporter dataImporter = null;
        if(config.getGaeBaseUrl() != null && config.getGaeSecurityToken() != null){
            dataImporter = new DataImporter(config.getGaeBaseUrl(), config.getGaeSecurityToken());
        }


        UpdateSite.Data data = config.getWar() == null ? extractUpdateCenterData() : scanWAR(config.getWar());
        PluginCompatReport report = PluginCompatReport.fromXml(config.reportFile);

        SortedSet<MavenCoordinates> testedCores = config.getWar() == null ? generateCoreCoordinatesToTest(data, report) : coreVersionFromWAR(data);

        MavenRunner.Config mconfig = new MavenRunner.Config();
        mconfig.userSettingsFile = config.getM2SettingsFile();
        // TODO REMOVE
        mconfig.userProperties.put( "failIfNoTests", "false" );
        mconfig.userProperties.put( "argLine", "-XX:MaxPermSize=128m" );
        String mavenPropertiesFilePath = this.config.getMavenPropertiesFile();
        if ( StringUtils.isNotBlank( mavenPropertiesFilePath )) {
            File file = new File (mavenPropertiesFilePath);
            if (file.exists()) {
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream( file );
                    Properties properties = new Properties(  );
                    properties.load( fileInputStream  );
                    for (Map.Entry<Object,Object> entry : properties.entrySet()) {
                        mconfig.userProperties.put((String) entry.getKey(), (String) entry.getValue());
                    }
                } finally {
                    IOUtils.closeQuietly( fileInputStream );
                }
            } else {
                System.out.println("File " + mavenPropertiesFilePath + " not exists" );
            }
        }

		SCMManagerFactory.getInstance().start();
        for(MavenCoordinates coreCoordinates : testedCores){
            System.out.println("Starting plugin tests on core coordinates : "+coreCoordinates.toString());
            for (Plugin plugin : data.plugins.values()) {
                if(config.getIncludePlugins()==null || config.getIncludePlugins().contains(plugin.name.toLowerCase())){
                    PluginInfos pluginInfos = new PluginInfos(plugin.name, plugin.version, plugin.url);

                    if(config.getExcludePlugins()!=null && config.getExcludePlugins().contains(plugin.name.toLowerCase())){
                        System.out.println("Plugin "+plugin.name+" is in excluded plugins => test skipped !");
                        continue;
                    }

                    String errorMessage = null;
                    TestStatus status = null;

                    MavenCoordinates actualCoreCoordinates = coreCoordinates;
                    PluginRemoting remote = new PluginRemoting(plugin.url);
                    PomData pomData;
                    try {
                        pomData = remote.retrievePomData();
                        System.out.println("detected parent POM " + pomData.parent.toGAV());
                        if ((pomData.parent.groupId.equals(PluginCompatTesterConfig.DEFAULT_PARENT_GROUP)
                                && pomData.parent.artifactId.equals(PluginCompatTesterConfig.DEFAULT_PARENT_ARTIFACT)
                                || pomData.parent.groupId.equals("org.jvnet.hudson.plugins"))
                                && coreCoordinates.version.matches("1[.][0-9]+[.][0-9]+")
                                && new VersionNumber(coreCoordinates.version).compareTo(new VersionNumber("1.485")) < 0) { // TODO unless 1.480.3+
                            System.out.println("Cannot test against " + coreCoordinates.version + " due to lack of deployed POM for " + coreCoordinates.toGAV());
                            actualCoreCoordinates = new MavenCoordinates(coreCoordinates.groupId, coreCoordinates.artifactId, coreCoordinates.version.replaceFirst("[.][0-9]+$", ""));
                        }
                    } catch (Throwable t) {
                        status = TestStatus.INTERNAL_ERROR;
                        errorMessage = t.getMessage();
                        pomData = null;
                    }

                    if(!config.isSkipTestCache() && report.isCompatTestResultAlreadyInCache(pluginInfos, actualCoreCoordinates, config.getTestCacheTimeout(), config.getCacheThresholStatus())){
                        System.out.println("Cache activated for plugin "+pluginInfos.pluginName+" => test skipped !");
                        continue; // Don't do anything : we are in the cached interval ! :-)
                    }

                    List<String> warningMessages = new ArrayList<String>();
                    if (errorMessage == null) {
                    try {
                        TestExecutionResult result = testPluginAgainst(actualCoreCoordinates, plugin, mconfig, pomData);
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
                    }


                    File buildLogFile = createBuildLogFile(config.reportFile, plugin.name, plugin.version, actualCoreCoordinates);
                    String buildLogFilePath = "";
                    if(buildLogFile.exists()){
                        buildLogFilePath = createBuildLogFilePathFor(pluginInfos.pluginName, pluginInfos.pluginVersion, actualCoreCoordinates);
                    }
                    PluginCompatResult result = new PluginCompatResult(actualCoreCoordinates, status, errorMessage, warningMessages, buildLogFilePath);
                    report.add(pluginInfos, result);

                    // Adding result to GAE
                    if(dataImporter != null){
                        dataImporter.importPluginCompatResult(result, pluginInfos, config.reportFile.getParentFile());
                        // TODO: import log files
                    }

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

    private void generateHtmlReportFile() throws IOException {
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

    private static File createBuildLogFile(File reportFile, String pluginName, String pluginVersion, MavenCoordinates coreCoords){
        return new File(reportFile.getParentFile().getAbsolutePath()
                            +"/"+createBuildLogFilePathFor(pluginName, pluginVersion, coreCoords));
    }

    private static String createBuildLogFilePathFor(String pluginName, String pluginVersion, MavenCoordinates coreCoords){
        return String.format("logs/%s/v%s_against_%s_%s_%s.log", pluginName, pluginVersion, coreCoords.groupId, coreCoords.artifactId, coreCoords.version);
    }
	
	private TestExecutionResult testPluginAgainst(MavenCoordinates coreCoordinates, Plugin plugin, MavenRunner.Config mconfig, PomData pomData)
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
		
		try {
            System.out.println("Checking out from SCM connection URL : "+pomData.getConnectionUrl()+" ("+plugin.name+"-"+plugin.version+")");
			ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
			ScmRepository repository = scmManager.makeScmRepository(pomData.getConnectionUrl());
			CheckOutScmResult result = scmManager.checkOut(repository, new ScmFileSet(pluginCheckoutDir), new ScmTag(plugin.name+"-"+plugin.version));
			
			if(!result.isSuccess()){
                if(result.getCommandOutput().contains("error: pathspec") && result.getCommandOutput().contains("did not match any file(s) known to git.")){
                    // Trying to look for existing branch that looks like the one we are looking for
                    // TODO ???
                } else {
                    throw new RuntimeException(result.getProviderMessage() + "||" + result.getCommandOutput());
                }
			}
		} catch (ComponentLookupException e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem while creating ScmManager !", e);
		} catch (Exception e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem while checking out plugin sources!", e);
		}
		
        List<String> baseArgs = new ArrayList<String>();
        boolean mustTransformPom = false;
        // TODO future versions of DEFAULT_PARENT_GROUP/ARTIFACT may be able to use this as well
        if (pomData.parent.groupId.equals("com.cloudbees.jenkins.plugins") && pomData.parent.artifactId.equals("jenkins-plugins")) {
            baseArgs.add("-Djenkins.version=" + coreCoordinates.version);
            baseArgs.add("-Dhpi-plugin.version=1.99"); // TODO would ideally pick up exact version from org.jenkins-ci.main:pom
        } else {
            mustTransformPom = true;
        }

        File buildLogFile = createBuildLogFile(config.reportFile, plugin.name, plugin.version, coreCoordinates);
        FileUtils.forceMkdir(buildLogFile.getParentFile()); // Creating log directory
        FileUtils.fileWrite(buildLogFile.getAbsolutePath(), ""); // Creating log file

        boolean ranCompile = false;
        try {
            // First build against the original POM.
            // This defends against source incompatibilities (which we do not care about for this purpose);
            // and ensures that we are testing a plugin binary as close as possible to what was actually released.
            List<String> args = new ArrayList<String>(baseArgs);
            args.add("clean");
            args.add("process-test-classes");
            runner.run(mconfig, pluginCheckoutDir, buildLogFile, args.toArray(new String[args.size()]));
            ranCompile = true;

            // Then transform the POM and run tests against that.
            // You might think that it would suffice to run e.g.
            // -Dmaven-surefire-plugin.version=2.15 -Dmaven.test.dependency.excludes=org.jenkins-ci.main:jenkins-war -Dmaven.test.additionalClasspath=/…/org/jenkins-ci/main/jenkins-war/1.580.1/jenkins-war-1.580.1.war clean test
            // (2.15+ required for ${maven.test.dependency.excludes} and ${maven.test.additionalClasspath} to be honored from CLI)
            // but it does not work; there are lots of linkage errors as some things are expected to be in the test classpath which are not.
            // Much simpler to do use the parent POM to set up the test classpath.
            MavenPom pom = new MavenPom(pluginCheckoutDir);
            try {
                addSplitPluginDependencies(mconfig, pluginCheckoutDir, pom);
            } catch (Exception x) {
                x.printStackTrace();
                pomData.getWarningMessages().add(Functions.printThrowable(x));
                // but continue
            }
            if (mustTransformPom) {
                pom.transformPom(coreCoordinates);
            }
            args = new ArrayList<String>(baseArgs);
            args.add("--define=maven.test.redirectTestOutputToFile=false");
            args.add("--define=concurrency=1");
            args.add("surefire:test");
            runner.run(mconfig, pluginCheckoutDir, buildLogFile, args.toArray(new String[args.size()]));

            return new TestExecutionResult(pomData.getWarningMessages());
        }catch(PomExecutionException e){
            PomExecutionException e2 = new PomExecutionException(e);
            e2.getPomWarningMessages().addAll(pomData.getWarningMessages());
            if (ranCompile) {
                // So the status is considered to be TEST_FAILURES not COMPILATION_ERROR:
                e2.succeededPluginArtifactIds.add("maven-compiler-plugin");
            }
            throw e2;
        }
	}

    private UpdateSite.Data extractUpdateCenterData(){
		URL url = null;
		String jsonp = null;
		try {
	        url = new URL(config.updateCenterUrl);
	        jsonp = IOUtils.toString(url.openStream());
		}catch(IOException e){
			throw new RuntimeException("Invalid update center url : "+config.updateCenterUrl, e);
		}
		
        String json = jsonp.substring(jsonp.indexOf('(')+1,jsonp.lastIndexOf(')'));
        UpdateSite us = new UpdateSite(DEFAULT_SOURCE_ID, url.toExternalForm());
        return newUpdateSiteData(us, JSONObject.fromObject(json));
	}

    private UpdateSite.Data scanWAR(File war) throws IOException {
        JSONObject top = new JSONObject();
        top.put("id", DEFAULT_SOURCE_ID);
        JSONObject plugins = new JSONObject();
        JarFile jf = new JarFile(war);
        try {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                Matcher m = Pattern.compile("WEB-INF/lib/jenkins-core-([0-9.]+(?:-SNAPSHOT)?)[.]jar").matcher(name);
                if (m.matches()) {
                    if (top.has("core")) {
                        throw new IOException(">1 jenkins-core.jar in " + war);
                    }
                    top.put("core", new JSONObject().accumulate("name", "core").accumulate("version", m.group(1)).accumulate("url", ""));
                }
                m = Pattern.compile("WEB-INF/plugins/([^/.]+)[.][hj]pi").matcher(name);
                if (m.matches()) {
                    JSONObject plugin = new JSONObject().accumulate("url", "").accumulate("dependencies", new JSONArray());
                    InputStream is = jf.getInputStream(entry);
                    try {
                        JarInputStream jis = new JarInputStream(is);
                        try {
                            Manifest manifest = jis.getManifest();
                            String shortName = manifest.getMainAttributes().getValue("Short-Name");
                            if (shortName == null) {
                                shortName = manifest.getMainAttributes().getValue("Extension-Name");
                                if (shortName == null) {
                                    shortName = m.group(1);
                                }
                            }
                            if (shortName.equals("maven-plugin")) {
                                continue; // this is special
                            }
                            plugin.put("name", shortName);
                            plugin.put("version", manifest.getMainAttributes().getValue("Plugin-Version"));
                            plugin.put("url", "jar:" + war.toURI() + "!/" + name);
                            plugins.put(shortName, plugin);
                        } finally {
                            jis.close();
                        }
                    } finally {
                        is.close();
                    }
                }
            }
        } finally {
            jf.close();
        }
        top.put("plugins", plugins);
        if (!top.has("core")) {
            throw new IOException("no jenkins-core.jar in " + war);
        }
        System.out.println("Scanned contents of " + war + ": " + top);
        return newUpdateSiteData(new UpdateSite(DEFAULT_SOURCE_ID, null), top);
    }

    private SortedSet<MavenCoordinates> coreVersionFromWAR(UpdateSite.Data data) {
        SortedSet<MavenCoordinates> result = new TreeSet<MavenCoordinates>();
        result.add(new MavenCoordinates(PluginCompatTesterConfig.DEFAULT_PARENT_GROUP, PluginCompatTesterConfig.DEFAULT_PARENT_ARTIFACT, data.core.version));
        return result;
    }

    private UpdateSite.Data newUpdateSiteData(UpdateSite us, JSONObject jsonO) throws RuntimeException {
        try {
            Constructor<UpdateSite.Data> dataConstructor = UpdateSite.Data.class.getDeclaredConstructor(UpdateSite.class, JSONObject.class);
            dataConstructor.setAccessible(true);
            return dataConstructor.newInstance(us, jsonO);
        }catch(Exception e){
            throw new RuntimeException("UpdateSite.Data instanciation problems", e);
        }
    }

    private void addSplitPluginDependencies(MavenRunner.Config mconfig, File pluginCheckoutDir, MavenPom pom) throws PomExecutionException, IOException {
        File tmp = File.createTempFile("dependencies", ".log");
        VersionNumber coreDep = null;
        Map<String,VersionNumber> pluginDeps = new HashMap<String,VersionNumber>();
        try {
            runner.run(mconfig, pluginCheckoutDir, tmp, "dependency:resolve");
            Reader r = new FileReader(tmp);
            try {
                BufferedReader br = new BufferedReader(r);
                Pattern p = Pattern.compile("\\[INFO\\]    ([^:]+):([^:]+):([a-z-]+):([^:]+):(provided|compile|runtime|test|system)");
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher m = p.matcher(line);
                    if (!m.matches()) {
                        continue;
                    }
                    String groupId = m.group(1);
                    String artifactId = m.group(2);
                    VersionNumber version;
                    try {
                        version = new VersionNumber(m.group(4));
                    } catch (IllegalArgumentException x) {
                        // OK, some other kind of dep, just ignore
                        continue;
                    }
                    if (groupId.equals("org.jenkins-ci.main") && artifactId.equals("jenkins-core")) {
                        coreDep = version;
                    } else if (groupId.equals("org.jenkins-ci.plugins")) { // ignore org.jenkins-ci.main:maven-plugin
                        pluginDeps.put(artifactId, version);
                    }
                }
            } finally {
                r.close();
            }
        } finally {
            tmp.delete();
        }
        if (coreDep != null) {
            // Synchronize with ClassicPluginStrategy.DETACHED_LIST:
            String[] splits = {
                // too special: "maven-plugin:1.296:1.296",
                "subversion:1.310:1.0",
                "cvs:1.340:0.1",
                "ant:1.430.*:1.0",
                "javadoc:1.430.*:1.0",
                "external-monitor-job:1.467.*:1.0",
                "ldap:1.467.*:1.0",
                "pam-auth:1.467.*:1.0",
                "mailer:1.493.*:1.2",
                "matrix-auth:1.535.*:1.0.2",
            };
            Map<String,VersionNumber> toAdd = new HashMap<String,VersionNumber>();
            for (String split : splits) {
                String[] pieces = split.split(":");
                // TODO this should only happen if the tested core version is ≥ pieces[1]
                if (coreDep.compareTo(new VersionNumber(pieces[1])) <= 0 && !pluginDeps.containsKey(pieces[0])) {
                    // TODO should be use the split version, or the current version in jenkins.war?
                    toAdd.put(pieces[0], new VersionNumber(pieces[2]));
                }
            }
            if (!toAdd.isEmpty()) {
                System.out.println("Adding plugin dependencies for compatibility: " + toAdd);
                pom.addDependencies(toAdd, coreDep);
            }
        }
    }

}
