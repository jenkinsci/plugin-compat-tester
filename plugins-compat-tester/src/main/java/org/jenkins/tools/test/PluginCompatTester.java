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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.Functions;
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import hudson.model.UpdateSite.Plugin;
import hudson.util.VersionNumber;
import io.jenkins.lib.versionnumber.JavaSpecificationVersion;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
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
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.exception.ExecutedTestNamesSolverException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.model.MavenBom;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.MavenPom;
import org.jenkins.tools.test.model.PCTPlugin;
import org.jenkins.tools.test.model.PluginCompatReport;
import org.jenkins.tools.test.model.PluginCompatResult;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PluginInfos;
import org.jenkins.tools.test.model.PluginRemoting;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.TestExecutionResult;
import org.jenkins.tools.test.model.TestStatus;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCompile;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHooks;
import org.jenkins.tools.test.util.ExecutedTestNamesSolver;
import org.springframework.core.io.ClassPathResource;

/**
 * Frontend for plugin compatibility tests
 *
 * @author Frederic Camblor, Olivier Lamy
 */
public class PluginCompatTester {

    private static final Logger LOGGER = Logger.getLogger(PluginCompatTester.class.getName());
    private static final String DEFAULT_SOURCE_ID = "default";

    /** First version with new parent POM. */
    public static final String JENKINS_CORE_FILE_REGEX = "WEB-INF/lib/jenkins-core-([0-9.]+(?:-[0-9a-f.]+)*(?:-(?i)([a-z]+)(-)?([0-9a-f.]+)?)?(?:-(?i)([a-z]+)(-)?([0-9a-f.]+)?)?(?:-SNAPSHOT)?)[.]jar";

    private PluginCompatTesterConfig config;
    private final ExternalMavenRunner runner;

    private List<String> splits;
    private Set<String> splitCycles;

    public PluginCompatTester(PluginCompatTesterConfig config){
        this.config = config;
        runner = new ExternalMavenRunner(config.getExternalMaven());
    }

    private SortedSet<MavenCoordinates> generateCoreCoordinatesToTest(UpdateSite.Data data, PluginCompatReport previousReport){
        SortedSet<MavenCoordinates> coreCoordinatesToTest;
        // If parent GroupId/Artifact are not null, this will be fast : we will only test
        // against 1 core coordinate
        if(config.getParentGroupId() != null && config.getParentArtifactId() != null){
            coreCoordinatesToTest = new TreeSet<>();

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
                    "config.parentGroupId=" + config.getParentGroupId() + ", config.parentArtifactId=" + config.getParentArtifactId());
        }

        return coreCoordinatesToTest;
    }

	public PluginCompatReport testPlugins()
            throws PlexusContainerException, IOException, PomExecutionException, XmlPullParserException {
        File war = config.getWar();
        if (war != null) {
            populateSplits(war);
        } else {
            // TODO find a way to load the local version of jenkins.war acc. to UC metadata
            splits = HISTORICAL_SPLITS;
            splitCycles = HISTORICAL_SPLIT_CYCLES;
        }

        PluginCompatTesterHooks pcth = new PluginCompatTesterHooks(config.getHookPrefixes());
        // Providing XSL Stylesheet along xml report file
        if(config.reportFile != null){
            if(config.isProvideXslReport()){
                File xslFilePath = PluginCompatReport.getXslFilepath(config.reportFile);
                FileUtils.copyStreamToFile(new RawInputStreamFacade(getXslTransformerResource().getInputStream()), xslFilePath);
            }
        }

        // Determine the plugin data
        HashMap<String,String> pluginGroupIds = new HashMap<>();  // Used to track real plugin groupIds from WARs

        // Scan bundled plugins
        // If there is any bundled plugin, only these plugins will be taken under the consideration for the PCT run
        UpdateSite.Data data = null;
        if (config.getBom() != null) {
            data = scanBom(pluginGroupIds, "([^/.]+)[.][hj]pi");
        } else {
            data = config.getWar() == null ? extractUpdateCenterData(pluginGroupIds) : scanWAR(config.getWar(), pluginGroupIds, "WEB-INF/(?:optional-)?plugins/([^/.]+)[.][hj]pi");
        }
        if (!data.plugins.isEmpty()) {
            // Scan detached plugins to recover proper Group IDs for them
            // At the moment, we are considering that bomfile contains the info about the detached ones
            UpdateSite.Data detachedData = config.getBom() != null ? null : config.getWar() != null ? scanWAR(config.getWar(), pluginGroupIds, "WEB-INF/(?:detached-)?plugins/([^/.]+)[.][hj]pi") : extractUpdateCenterData(pluginGroupIds);

            // Add detached if and only if no added as normal one
            UpdateSite.Data finalData = data;
            if (detachedData != null) {
                detachedData.plugins.forEach((key, value) -> {
                    if (!finalData.plugins.containsKey(key)) {
                        finalData.plugins.put(key, value);
                    }
                });
            }
        }

        final Map<String, Plugin> pluginsToCheck;
        final List<String> pluginsToInclude = config.getIncludePlugins();
        if (data.plugins.isEmpty() && pluginsToInclude != null && !pluginsToInclude.isEmpty()) {
            // Update Center returns empty info OR the "-war" option is specified for WAR without bundled plugins
            System.out.println("WAR file does not contain plugin info, will try to extract it from UC for included plugins");
            pluginsToCheck = new HashMap<>(pluginsToInclude.size());
            UpdateSite.Data ucData = extractUpdateCenterData(pluginGroupIds);
            for (String plugin : pluginsToInclude) {
                UpdateSite.Plugin pluginData = ucData.plugins.get(plugin);
                if (pluginData != null) {
                    System.out.println("Adding " + plugin + " to the test scope");
                    pluginsToCheck.put(plugin, pluginData);
                }
            }
        } else {
            pluginsToCheck = data.plugins;
        }

        if (pluginsToCheck.isEmpty()) {
            throw new IOException("List of plugins to check is empty, it is not possible to run PCT");
        }

        PluginCompatReport report = PluginCompatReport.fromXml(config.reportFile);

        SortedSet<MavenCoordinates> testedCores = config.getWar() == null ? generateCoreCoordinatesToTest(data, report) : coreVersionFromWAR(data);

        MavenRunner.Config mconfig = new MavenRunner.Config();
        mconfig.userSettingsFile = config.getM2SettingsFile();
        // TODO REMOVE
        mconfig.userProperties.put( "failIfNoTests", "false" );
        mconfig.userProperties.putAll(this.config.retrieveMavenProperties());
        report.setTestJavaVersion(config.getTestJavaVersion());

        boolean failed = false;
        SCMManagerFactory.getInstance().start();
        ROOT_CYCLE: for(MavenCoordinates coreCoordinates : testedCores){
            System.out.println("Starting plugin tests on core coordinates : "+coreCoordinates.toString());
            for (Plugin plugin : pluginsToCheck.values()) {
                if(config.getIncludePlugins()==null || config.getIncludePlugins().contains(plugin.name.toLowerCase())){
                    PluginInfos pluginInfos = new PluginInfos(plugin.name, plugin.version, plugin.url);

                    if(config.getExcludePlugins()!=null && config.getExcludePlugins().contains(plugin.name.toLowerCase())){
                        System.out.println("Plugin "+plugin.name+" is in excluded plugins => test skipped !");
                        continue;
                    }

                    String errorMessage = null;
                    TestStatus status = null;

                    MavenCoordinates actualCoreCoordinates = coreCoordinates;
                    PluginRemoting remote;
                    if (localCheckoutProvided() && onlyOnePluginIncluded()) {
                        // Only one plugin and checkout directory provided
                        remote = new PluginRemoting(new File(config.getLocalCheckoutDir(), "pom.xml"));
                    } else if(localCheckoutProvided()) {
                        // local directory provided for more than one plugin, so each plugin is allocated in localCheckoutDir/plugin-name
                        // If there is no subdirectory for the plugin, it will be cloned from scm
                        File pomFile = new File(new File(config.getLocalCheckoutDir(), plugin.name), "pom.xml");
                        if (pomFile.exists()) {
                            remote = new PluginRemoting(pomFile);
                        } else {
                            remote = new PluginRemoting(plugin.url);
                        }
                    } else {
                        // Only one plugin but checkout directory not provided or
                        // more than a plugin and no local checkout directory provided
                        remote = new PluginRemoting(plugin.url);
                    }
                    PomData pomData;
                    try {
                        pomData = remote.retrievePomData();
                        MavenCoordinates parentPom = pomData.parent;
                        if (parentPom != null) {
                            // Parent POM is used here only to detect old versions of core
                            LOGGER.log(Level.INFO,"Detected parent POM: {0}", parentPom.toGAV());
                            if ((parentPom.groupId.equals(PluginCompatTesterConfig.DEFAULT_PARENT_GROUP)
                                    && parentPom.artifactId.equals(PluginCompatTesterConfig.DEFAULT_PARENT_ARTIFACT)
                                    || parentPom.groupId.equals("org.jvnet.hudson.plugins"))
                                    && coreCoordinates.version.matches("1[.][0-9]+[.][0-9]+")
                                    && new VersionNumber(coreCoordinates.version).compareTo(new VersionNumber("1.485")) < 0) { // TODO unless 1.480.3+
                                LOGGER.log(Level.WARNING, "Cannot test against " + coreCoordinates.version + " due to lack of deployed POM for " + coreCoordinates.toGAV());
                                actualCoreCoordinates = new MavenCoordinates(coreCoordinates.groupId, coreCoordinates.artifactId, coreCoordinates.version.replaceFirst("[.][0-9]+$", ""));
                            }
                        }
                    } catch (Throwable t) {
                        status = TestStatus.INTERNAL_ERROR;
                        LOGGER.log(Level.SEVERE, String.format("Internal error while executing a test for core %s and plugin %s %s. Please submit a bug to plugin-compat-tester",
                                coreCoordinates.version, plugin.getDisplayName(), plugin.version), t);
                        errorMessage = t.getMessage();
                        pomData = null;
                    }

                    if(!config.isSkipTestCache() && report.isCompatTestResultAlreadyInCache(pluginInfos, actualCoreCoordinates, config.getTestCacheTimeout(), config.getCacheThresholdStatus())){
                        System.out.println("Cache activated for plugin "+pluginInfos.pluginName+" => test skipped !");
                        continue; // Don't do anything : we are in the cached interval ! :-)
                    }

                    List<String> warningMessages = new ArrayList<>();
                    Set<String> testDetails = new TreeSet<>();
                    if (errorMessage == null) {
                    try {
                        TestExecutionResult result = testPluginAgainst(actualCoreCoordinates, plugin, mconfig, pomData, pluginsToCheck, pluginGroupIds, pcth, config.getOverridenPlugins());
                        // If no PomExecutionException, everything went well...
                        status = TestStatus.SUCCESS;
                        warningMessages.addAll(result.pomWarningMessages);
                        testDetails.addAll(config.isStoreAll() ? result.getTestDetails().getAll() : result.getTestDetails().getFailed());
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
                        testDetails.addAll(config.isStoreAll() ? e.getTestDetails().getAll() : e.getTestDetails().getFailed());
                    } catch (Error e){
                        // Rethrow the error ... something is wrong !
                        throw e;
                    } catch (Throwable t){
                        status = TestStatus.INTERNAL_ERROR;
                        LOGGER.log(Level.SEVERE, String.format("Internal error while executing a test for core %s and plugin %s %s. Please submit a bug to plugin-compat-tester",
                                coreCoordinates.version, plugin.getDisplayName(), plugin.version), t);
                        errorMessage = t.getMessage();
                    }
                    }


                    File buildLogFile = createBuildLogFile(config.reportFile, plugin.name, plugin.version, actualCoreCoordinates);
                    String buildLogFilePath = "";
                    if(buildLogFile.exists()){
                        buildLogFilePath = createBuildLogFilePathFor(pluginInfos.pluginName, pluginInfos.pluginVersion, actualCoreCoordinates);
                    }

                    if(config.getBom() != null) {
                    	actualCoreCoordinates = new MavenCoordinates(actualCoreCoordinates.groupId, actualCoreCoordinates.artifactId, solveVersionFromModel(new MavenBom(config.getBom()).getModel()));
                    }

                    PluginCompatResult result = new PluginCompatResult(actualCoreCoordinates, status, errorMessage, warningMessages, testDetails, buildLogFilePath);
                    report.add(pluginInfos, result);

                    if(config.reportFile != null){
                        if(!config.reportFile.exists()){
                            FileUtils.fileWrite(config.reportFile.getAbsolutePath(), "");
                        }
                        report.save(config.reportFile);
                    }

                    if (status != TestStatus.SUCCESS) {
                        failed = true;
                        if (config.isFailOnError()) {
                            break ROOT_CYCLE;
                        }
                    }
                } else {
                    System.out.println("Plugin "+plugin.name+" not in included plugins => test skipped !");
                }
            }
        }

        // Generating HTML report only if needed, if the file does not exist is because no test has been executed
        if(config.isGenerateHtmlReport() && config.reportFile != null && config.reportFile.exists()) {
            generateHtmlReportFile();
        } else {
            System.out.println("No HTML report is generated, because it has been disabled or no tests have been executed");
        }

        if (failed && config.isFailOnError()) {
            throw new AbortException("Execution was aborted due to the failure in a plugin test (-failOnError is set)");
        }

        return report;
    }

    protected void generateHtmlReportFile() throws IOException {
        if (!config.reportFile.exists() || !config.reportFile.isFile()) {
            throw new FileNotFoundException("Cannot find the XML report file: " + config.reportFile);
        }

        Source xmlSource = new StreamSource(config.reportFile);
        try(InputStream xsltStream = getXslTransformerResource().getInputStream()) {
            Source xsltSource = new StreamSource(xsltStream);
            Result result = new StreamResult(PluginCompatReport.getHtmlFilepath(config.reportFile));

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer;
            try {
                transformer = factory.newTransformer(xsltSource);
                transformer.transform(xmlSource, result);
            } catch (TransformerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static ClassPathResource getXslTransformerResource(){
        return new ClassPathResource("resultToReport.xsl");
    }

    private static File createBuildLogFile(File reportFile, String pluginName, String pluginVersion, MavenCoordinates coreCoords){
        return new File(reportFile.getParentFile().getAbsolutePath()
                            + File.separator + createBuildLogFilePathFor(pluginName, pluginVersion, coreCoords));
    }

    private static String createBuildLogFilePathFor(String pluginName, String pluginVersion, MavenCoordinates coreCoords){
        return String.format("logs/%s/v%s_against_%s_%s_%s.log", pluginName, pluginVersion, coreCoords.groupId, coreCoords.artifactId, coreCoords.version);
    }

    private TestExecutionResult testPluginAgainst(MavenCoordinates coreCoordinates, Plugin plugin, MavenRunner.Config mconfig, PomData pomData, Map<String, Plugin> otherPlugins, Map<String, String> pluginGroupIds, PluginCompatTesterHooks pcth, List<PCTPlugin> overridenPlugins)
        throws PluginSourcesUnavailableException, PomExecutionException, IOException
    {
        System.out.println(String.format("%n%n%n%n%n"));
        System.out.println("#############################################");
        System.out.println("#############################################");
        System.out.println(String.format("##%n## Starting to test plugin %s v%s%n## against %s%n##", plugin.name, plugin.version, coreCoordinates));
        System.out.println("#############################################");
        System.out.println("#############################################");
        System.out.println(String.format("%n%n%n%n%n"));

        File pluginCheckoutDir = new File(config.workDirectory.getAbsolutePath() + File.separator + plugin.name + File.separator);

        try {
            // Run any precheckout hooks
            Map<String, Object> beforeCheckout = new HashMap<>();
            beforeCheckout.put("pluginName", plugin.name);
            beforeCheckout.put("plugin", plugin);
            beforeCheckout.put("pomData", pomData);
            beforeCheckout.put("config", config);
            beforeCheckout.put("runCheckout", true);
            beforeCheckout = pcth.runBeforeCheckout(beforeCheckout);

            if(beforeCheckout.get("executionResult") != null) { // Check if the hook returned a result
                return (TestExecutionResult)beforeCheckout.get("executionResult");
            } else if((boolean)beforeCheckout.get("runCheckout")) {
                if(beforeCheckout.get("checkoutDir") != null){
                    pluginCheckoutDir = (File)beforeCheckout.get("checkoutDir");
                }
                if (Files.isDirectory(pluginCheckoutDir.toPath())) {
                    System.out.println("Deleting working directory "+pluginCheckoutDir.getAbsolutePath());
                    FileUtils.deleteDirectory(pluginCheckoutDir);
                }

                Files.createDirectory(pluginCheckoutDir.toPath());
                System.out.println("Created plugin checkout dir : "+pluginCheckoutDir.getAbsolutePath());

                if (localCheckoutProvided()) {
                    if (!onlyOnePluginIncluded()) {
                        File localCheckoutPluginDir = new File(config.getLocalCheckoutDir(), plugin.name);
                        File pomLocalCheckoutPluginDir = new File(localCheckoutPluginDir, "pom.xml");
                        if(pomLocalCheckoutPluginDir.exists()) {
                            System.out.println("Copy plugin directory from : " + localCheckoutPluginDir.getAbsolutePath());
                            FileUtils.copyDirectoryStructure(localCheckoutPluginDir, pluginCheckoutDir);
                        } else {
                            cloneFromSCM(pomData, plugin.name, plugin.version, pluginCheckoutDir);
                        }
                    } else {
                        // TODO this fails when it encounters symlinks (e.g. work/jobs/…/builds/lastUnstableBuild),
                        // and even up-to-date versions of org.apache.commons.io.FileUtils seem to not handle links,
                        // so may need to use something like http://docs.oracle.com/javase/tutorial/displayCode.html?code=http://docs.oracle.com/javase/tutorial/essential/io/examples/Copy.java
                        System.out.println("Copy plugin directory from : " + config.getLocalCheckoutDir().getAbsolutePath());
                        FileUtils.copyDirectoryStructure(config.getLocalCheckoutDir(), pluginCheckoutDir);
                    }
                } else {
                    // These hooks could redirect the SCM, skip checkout (if multiple plugins use the same preloaded repo)
                    cloneFromSCM(pomData, plugin.name, plugin.version, pluginCheckoutDir);
                }
            } else {
                // If the plugin exists in a different directory (multimodule plugins)
                if (beforeCheckout.get("pluginDir") != null) {
                    pluginCheckoutDir = (File)beforeCheckout.get("checkoutDir");
                }
                System.out.println("The plugin has already been checked out, likely due to a multimodule situation. Continue.");
            }
        } catch (ComponentLookupException e) {
            System.err.println("Error : " + e.getMessage());
            throw new PluginSourcesUnavailableException("Problem while creating ScmManager !", e);
        } catch (Exception e) {
            System.err.println("Error : " + e.getMessage());
            throw new PluginSourcesUnavailableException("Problem while checking out plugin sources!", e);
        }

        File buildLogFile = createBuildLogFile(config.reportFile, plugin.name, plugin.version, coreCoordinates);
        FileUtils.forceMkdir(buildLogFile.getParentFile()); // Creating log directory
        FileUtils.fileWrite(buildLogFile.getAbsolutePath(), ""); // Creating log file

        // Ran the BeforeCompileHooks
        Map<String, Object> beforeCompile = new HashMap<>();
        beforeCompile.put("pluginName", plugin.name);
        beforeCompile.put("plugin", plugin);
        beforeCompile.put("pluginDir", pluginCheckoutDir);
        beforeCompile.put("pomData", pomData);
        beforeCompile.put("config", config);
        beforeCompile.put("core", coreCoordinates);
        Map<String, Object> hookInfo = pcth.runBeforeCompilation(beforeCompile);

        boolean ranCompile = hookInfo.containsKey(PluginCompatTesterHookBeforeCompile.OVERRIDE_DEFAULT_COMPILE) && (boolean) hookInfo.get(PluginCompatTesterHookBeforeCompile.OVERRIDE_DEFAULT_COMPILE);
        try {
            // First build against the original POM.
            // This defends against source incompatibilities (which we do not care about for this purpose);
            // and ensures that we are testing a plugin binary as close as possible to what was actually released.
            // We also skip potential javadoc execution to avoid general test failure.
            if (!ranCompile) {
                runner.run(mconfig, pluginCheckoutDir, buildLogFile, "clean", "process-test-classes", "-Dmaven.javadoc.skip");
            }
            ranCompile = true;

            // Then transform the POM and run tests against that.
            // You might think that it would suffice to run e.g.
            // -Dmaven-surefire-plugin.version=2.15 -Dmaven.test.dependency.excludes=org.jenkins-ci.main:jenkins-war -Dmaven.test.additionalClasspath=/…/org/jenkins-ci/main/jenkins-war/1.580.1/jenkins-war-1.580.1.war clean test
            // (2.15+ required for ${maven.test.dependency.excludes} and ${maven.test.additionalClasspath} to be honored from CLI)
            // but it does not work; there are lots of linkage errors as some things are expected to be in the test classpath which are not.
            // Much simpler to do use the parent POM to set up the test classpath.
            MavenPom pom = new MavenPom(pluginCheckoutDir);
            try {
                addSplitPluginDependencies(plugin.name, mconfig, pluginCheckoutDir, pom, otherPlugins, pluginGroupIds, coreCoordinates.version, overridenPlugins);
            } catch (Exception x) {
                x.printStackTrace();
                pomData.getWarningMessages().add(Functions.printThrowable(x));
                // but continue
            }

            List<String> args = new ArrayList<>();
            args.add("--define=forkCount=1");
            args.add("hpi:resolve-test-dependencies");
            args.add("hpi:test-hpl");
            args.add("surefire:test");

            // Run preexecution hooks
            Map<String, Object> forExecutionHooks = new HashMap<>();
            forExecutionHooks.put("pluginName", plugin.name);
            forExecutionHooks.put("args", args);
            forExecutionHooks.put("pomData", pomData);
            forExecutionHooks.put("pom", pom);
            forExecutionHooks.put("coreCoordinates", coreCoordinates);
            forExecutionHooks.put("config", config);
            forExecutionHooks.put("pluginDir", pluginCheckoutDir);
            pcth.runBeforeExecution(forExecutionHooks);
            args = (List<String>)forExecutionHooks.get("args");

            // Execute with tests
            runner.run(mconfig, pluginCheckoutDir, buildLogFile, args.toArray(new String[args.size()]));
            // TODO extract tests names by filter
            return new TestExecutionResult(((PomData)forExecutionHooks.get("pomData")).getWarningMessages(), new ExecutedTestNamesSolver().solve(runner.getExecutedTests(), pluginCheckoutDir));
        } catch (ExecutedTestNamesSolverException e) {
            throw new PomExecutionException(e);
        } catch (PomExecutionException e){
            e.getPomWarningMessages().addAll(pomData.getWarningMessages());
            if (ranCompile) {
                // So the status is considered to be TEST_FAILURES not COMPILATION_ERROR:
                e.succeededPluginArtifactIds.add("maven-compiler-plugin");
            }
            throw e;
        }
    }

    protected void cloneFromSCM(PomData pomData, String name, String version, File checkoutDirectory) throws ComponentLookupException, ScmException, IOException {
        String scmTag;
        if (pomData.getScmTag() != null) {
            scmTag = pomData.getScmTag();
            System.out.println(String.format("Using SCM tag '%s' from POM.", scmTag));
        } else {
            scmTag = name + "-" + version;
            System.out.println(String.format("POM did not provide an SCM tag. Inferring tag '%s'.", scmTag));
        }
        System.out.println("Checking out from SCM connection URL : " + pomData.getConnectionUrl() + " (" + name + "-" + version + ") at tag " + scmTag);
        ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
        ScmRepository repository = scmManager.makeScmRepository(pomData.getConnectionUrl());
        CheckOutScmResult result = scmManager.checkOut(repository, new ScmFileSet(checkoutDirectory), new ScmTag(scmTag));

        if (!result.isSuccess() && config.getFallbackGitHubOrganization() != null) {
            System.out.println("Using fallback organization in github: " + config.getFallbackGitHubOrganization());
            if (checkoutDirectory.isDirectory()) {
                FileUtils.deleteDirectory(checkoutDirectory);
            }

            Pattern pattern = Pattern.compile("(.*/github.com/)([^/]*)(.*)");
            Matcher matcher = pattern.matcher(pomData.getConnectionUrl());
            matcher.find();
            checkoutFallbackOrganization(scmManager, matcher, checkoutDirectory, scmTag);
        }
        else if (!result.isSuccess()) {
            throw new RuntimeException(result.getProviderMessage() + " || " + result.getCommandOutput());
        }
    }

    private void checkoutFallbackOrganization(ScmManager scmManager, Matcher matcher, File checkoutDirectory, String scmTag) throws ScmException{
        String connectionURL = matcher.replaceFirst("$1" + config.getFallbackGitHubOrganization() + "$3");
        System.out.println("Using fallback url in github: " + connectionURL);
        ScmRepository repository = scmManager.makeScmRepository(connectionURL);
        CheckOutScmResult result = scmManager.checkOut(repository, new ScmFileSet(checkoutDirectory), new ScmTag(scmTag));
        if (!result.isSuccess()) {
            connectionURL = matcher.replaceFirst("scm:git:git@github.com:" + config.getFallbackGitHubOrganization() + "$3");
            System.out.println("Using fallback url in github: " + connectionURL);
            repository = scmManager.makeScmRepository(connectionURL);
            result = scmManager.checkOut(repository, new ScmFileSet(checkoutDirectory), new ScmTag(scmTag));
            if (!result.isSuccess()) {
                throw new RuntimeException(result.getProviderMessage() + " || " + result.getCommandOutput());
            }
        }
    }

    private boolean localCheckoutProvided() {
        return config.getLocalCheckoutDir() != null && config.getLocalCheckoutDir().exists();
    }

    private boolean onlyOnePluginIncluded() {
        return config.getIncludePlugins() != null && config.getIncludePlugins().size() == 1;
    }

    /**
     * Extracts Update Site data from the update center.
     * @param groupIDs Target storage for Group IDs. The existing values won't be overridden
     * @return Update site Data
     */
    private UpdateSite.Data extractUpdateCenterData(Map<String, String> groupIDs){
        URL url;
        String jsonp;
        try {
            url = new URL(config.updateCenterUrl);
            jsonp = IOUtils.toString(url.openStream());
        }catch(IOException e){
            throw new RuntimeException("Invalid update center url : "+config.updateCenterUrl, e);
        }

        String json = jsonp.substring(jsonp.indexOf('(')+1,jsonp.lastIndexOf(')'));
        UpdateSite us = new UpdateSite(DEFAULT_SOURCE_ID, url.toExternalForm());

        JSONObject jsonObj = JSONObject.fromObject(json);
        UpdateSite.Data site = newUpdateSiteData(us, jsonObj);

        // UpdateSite.Plugin does not contain gav object, so we process the JSON object on our own here
        for(Map.Entry<String,JSONObject> e : (Set<Map.Entry<String,JSONObject>>)jsonObj.getJSONObject("plugins").entrySet()) {
            String gav = e.getValue().getString("gav");
            String groupId = gav.split(":")[0];
            groupIDs.putIfAbsent(e.getKey(), groupId);
        }

        return site;
    }

    private UpdateSite.Data scanBom(HashMap<String, String> pluginGroupIds, String pluginRegExp) throws IOException, PomExecutionException, XmlPullParserException {
    	
    	JSONObject top = new JSONObject();
    	top.put("id", DEFAULT_SOURCE_ID);
    	JSONObject plugins = new JSONObject();
    	
    	for (File entry : getBomEntries()) {
    		String name = entry.getName();
    		Matcher m = Pattern.compile(pluginRegExp).matcher(name);
    		try (InputStream is = new FileInputStream(entry); JarInputStream jis = new JarInputStream(is)) {
    		    Manifest manifest = jis.getManifest();
    		    if (manifest == null || manifest.getMainAttributes() == null) {
    		        // Skip this entry, is not a plugin and/or contains a malformed manifest so is not parseable
    		        System.out.println("Entry " + name + "defined in the BOM looks non parseable, ignoring");
    		        continue;
    		    }
    		    String jenkinsVersion = manifest.getMainAttributes().getValue("Jenkins-Version");
    		    String shortName = manifest.getMainAttributes().getValue("Short-Name");
    		    String groupId = manifest.getMainAttributes().getValue("Group-Id");
    		    String version = manifest.getMainAttributes().getValue("Plugin-Version");
    		    String dependencies = manifest.getMainAttributes().getValue("Plugin-Dependencies");
    		    // I expect BOMs to not specify hpi as type, which results in getting the jar artifact
    		    if (m.matches() || (jenkinsVersion != null && version != null)) { // I need a plugin version
    		        JSONObject plugin = new JSONObject().accumulate("url", "");
    		        if (shortName == null) {
    		            shortName = manifest.getMainAttributes().getValue("Extension-Name");
    		            if (shortName == null) {
    		                shortName = m.group(1);
    		            }
    		        }

    		        // If hpi is registered, avoid to override it by its jar entry
    		        if(plugins.containsKey(shortName) && entry.getPath().endsWith(".jar")) {
    		            continue;
    		        }

    		        plugin.put("name", shortName);
    		        pluginGroupIds.put(shortName, groupId);
    		        // Remove extra build information from the version number
    		        final Matcher matcher = Pattern.compile("^(.+-SNAPSHOT)(.+)$").matcher(version);
    		        if (matcher.matches()) {
    		            version = matcher.group(1);
    		        }
    		        plugin.put("version", version);
    		        plugin.put("url", "jar:" + entry.toURI().getPath() + "!/name.hpi");
    		        JSONArray dependenciesA = new JSONArray();
    		        if (dependencies != null) {
    		            // e.g. matrix-auth:1.0.2;resolution:=optional,credentials:1.8.3;resolution:=optional
    		            for (String pair : dependencies.split(",")) {
    		                boolean optional = pair.endsWith("resolution:=optional");
    		                String[] nameVer = pair.replace(";resolution:=optional", "").split(":");
    		                assert nameVer.length == 2;
    		                dependenciesA.add(new JSONObject().accumulate("name", nameVer[0]).accumulate("version", nameVer[1]).accumulate("optional", String.valueOf(optional)));
    		            }
    		        }
    		        plugin.accumulate("dependencies", dependenciesA);
    		        plugins.put(shortName, plugin);
    		    }
    		}
    	}

    	top.put("plugins", plugins);
    	if (!top.has("core")) {
    		// Not all boms have the jenkins core dependency explicit, so, assume the bom version matches the jenkins version
    		String core = solveCoreVersionFromBom();
    		if (StringUtils.isEmpty(core)) {
    			throw new IllegalStateException("Unable to retrieve any version for the core");
    		}
    		top.put("core", new JSONObject().accumulate("name", "core").accumulate("version",core).accumulate("url", "https://foobar"));
    	}
    	System.out.println("Readed contents of " + config.getBom() + ": " + top);
    	return newUpdateSiteData(new UpdateSite(DEFAULT_SOURCE_ID, null), top);
    }

    private List<File> getBomEntries() throws IOException, XmlPullParserException, PomExecutionException {
        File fullDepPom = new MavenBom(config.getBom()).writeFullDepPom(config.workDirectory);

    	MavenRunner.Config mconfig = new MavenRunner.Config();
    	mconfig.userSettingsFile = config.getM2SettingsFile();
    	System.out.println(mconfig.userSettingsFile);
    	// TODO REMOVE
    	mconfig.userProperties.putAll(this.config.retrieveMavenProperties());

    	File buildLogFile = new File(config.workDirectory
    			+ File.separator + "bom-download.log");
    	FileUtils.fileWrite(buildLogFile.getAbsolutePath(), ""); // Creating log file

    	runner.run(mconfig, fullDepPom.getParentFile(), buildLogFile, "dependency:copy-dependencies", "-P consume-incrementals", "-N");
    	return FileUtils.getFiles(new File(config.workDirectory, "bom" + File.separator + "target" + File.separator + "dependency"),null, null);
    }

    /**
     * @return Provides the core version from the bomfile and in case it is not found, the bom version, if bom version does not exist, it provides from its parent
     * @throws IOException
     * @throws XmlPullParserException
     */
    private String solveCoreVersionFromBom() throws IOException, XmlPullParserException {
    	Model model = new MavenBom(config.getBom()).getModel();
    	for (Dependency dependency : model.getDependencies()) {
		if(dependency.getArtifactId().equals("jenkins-core")) {
			return getProperty(model, dependency.getVersion());
		}
	}
    	return solveVersionFromModel(model);
    }

    private String solveVersionFromModel(Model model) {
	String version = model.getVersion();
	Parent parent = model.getParent();
	return version != null ? version : parent != null ? parent.getVersion() : StringUtils.EMPTY;
    }
    
    /**
     * Given a value and a model, it checks if it is an interpolated value. In negative case it returns the same 
     * value. In affirmative case, it retrieves its value from the properties of the Maven model.
     * @param model
     * @param version
     * @return the effective value of an specific value in a model
     */
    private String getProperty(Model model, String value) {
    	if (!value.contains("$")) {
    		return value;
    	}
    	
    	String key = value.replaceAll("\\$", "")
    			.replaceAll("\\{", "")
    			.replaceAll("\\}", "");
    	
    	return getProperty(model, model.getProperties().getProperty(key));
    }

    /**
     * Scans through a WAR file, accumulating plugin information
     * @param war WAR to scan
     * @param pluginGroupIds Map pluginName to groupId if set in the manifest, MUTATED IN THE EXECUTION
     * @param pluginRegExp The plugin regexp to use, can be used to differentiate between detached or "normal" plugins
     *                     in the war file
     * @return Update center data
     */
    private UpdateSite.Data scanWAR(File war, @Nonnull Map<String, String> pluginGroupIds, String pluginRegExp) throws IOException {
        JSONObject top = new JSONObject();
        top.put("id", DEFAULT_SOURCE_ID);
        JSONObject plugins = new JSONObject();
        try (JarFile jf = new JarFile(war)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                Matcher m = Pattern.compile(JENKINS_CORE_FILE_REGEX).matcher(name);
                if (m.matches()) {
                    if (top.has("core")) {
                        throw new IOException(">1 jenkins-core.jar in " + war);
                    }
                    // http://foobar is used to workaround the check in https://github.com/jenkinsci/jenkins/commit/f8daafd0327081186c06555f225e84c420261b4c
                    // We do not really care about the value
                    top.put("core", new JSONObject().accumulate("name", "core").accumulate("version", m.group(1)).accumulate("url", "https://foobar"));
                }

                m = Pattern.compile(pluginRegExp).matcher(name);
                if (m.matches()) {
                    JSONObject plugin = new JSONObject().accumulate("url", "");
                    try (InputStream is = jf.getInputStream(entry); JarInputStream jis = new JarInputStream(is)) {
                        Manifest manifest = jis.getManifest();
                        String shortName = manifest.getMainAttributes().getValue("Short-Name");
                        if (shortName == null) {
                            shortName = manifest.getMainAttributes().getValue("Extension-Name");
                            if (shortName == null) {
                                shortName = m.group(1);
                            }
                        }
                        plugin.put("name", shortName);
                        pluginGroupIds.put(shortName, manifest.getMainAttributes().getValue("Group-Id"));
                        String version = manifest.getMainAttributes().getValue("Plugin-Version");
                        // Remove extra build information from the version number
                        final Matcher matcher = Pattern.compile("^(.+-SNAPSHOT)(.+)$").matcher(version);
                        if (matcher.matches()) {
                            version = matcher.group(1);
                        }
                        plugin.put("version", version);
                        plugin.put("url", "jar:" + war.toURI() + "!/" + name);
                        JSONArray dependenciesA = new JSONArray();
                        String dependencies = manifest.getMainAttributes().getValue("Plugin-Dependencies");
                        if (dependencies != null) {
                            // e.g. matrix-auth:1.0.2;resolution:=optional,credentials:1.8.3;resolution:=optional
                            for (String pair : dependencies.split(",")) {
                                boolean optional = pair.endsWith("resolution:=optional");
                                String[] nameVer = pair.replace(";resolution:=optional", "").split(":");
                                assert nameVer.length == 2;
                                dependenciesA.add(new JSONObject().accumulate("name", nameVer[0]).accumulate("version", nameVer[1]).accumulate("optional", String.valueOf(optional)));
                            }
                        }
                        plugin.accumulate("dependencies", dependenciesA);
                        plugins.put(shortName, plugin);
                    }
                }
            }
        }
        top.put("plugins", plugins);
        if (!top.has("core")) {
            throw new IOException("no jenkins-core.jar in " + war);
        }
        System.out.println("Scanned contents of " + war + ": " + top);
        return newUpdateSiteData(new UpdateSite(DEFAULT_SOURCE_ID, null), top);
    }

    private SortedSet<MavenCoordinates> coreVersionFromWAR(UpdateSite.Data data) {
        SortedSet<MavenCoordinates> result = new TreeSet<>();
        result.add(new MavenCoordinates(PluginCompatTesterConfig.DEFAULT_PARENT_GROUP, PluginCompatTesterConfig.DEFAULT_PARENT_ARTIFACT, data.core.version));
        return result;
    }

    private UpdateSite.Data newUpdateSiteData(UpdateSite us, JSONObject jsonO) throws RuntimeException {
        try {
            Constructor<UpdateSite.Data> dataConstructor = UpdateSite.Data.class.getDeclaredConstructor(UpdateSite.class, JSONObject.class);
            dataConstructor.setAccessible(true);
            return dataConstructor.newInstance(us, jsonO);
        }catch(Exception e){
            throw new RuntimeException("UpdateSite.Data instantiation problems", e);
        }
    }

    private void addSplitPluginDependencies(String thisPlugin, MavenRunner.Config mconfig, File pluginCheckoutDir, MavenPom pom, Map<String, Plugin> otherPlugins, Map<String, String> pluginGroupIds, String coreVersion, List<PCTPlugin> overridenPlugins) throws PomExecutionException, IOException {
        File tmp = File.createTempFile("dependencies", ".log");
        VersionNumber coreDep = null;
        Map<String,VersionNumber> pluginDeps = new HashMap<>();
        Map<String,VersionNumber> pluginDepsTest = new HashMap<>();
        try {
            runner.run(mconfig, pluginCheckoutDir, tmp, "dependency:resolve");
            try (BufferedReader br =
                    Files.newBufferedReader(tmp.toPath(), Charset.defaultCharset())) {
                Pattern p = Pattern.compile("\\[INFO\\]([^:]+):([^:]+):([a-z-]+):(([^:]+):)?([^:]+):(provided|compile|runtime|system)(\\(optional\\))?");
                Pattern p2 = Pattern.compile("\\[INFO\\]([^:]+):([^:]+):([a-z-]+):(([^:]+):)?([^:]+):(test)");
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.replace(" ", "");
                    Matcher m = p.matcher(line);
                    Matcher m2 = p2.matcher(line);
                    String groupId;
                    String artifactId;
                    VersionNumber version;
                    if (!m.matches() && !m2.matches()) {
                        continue;
                    } else if (m.matches()) {
                        groupId = m.group(1);
                        artifactId = m.group(2);
                        try {
                            version = new VersionNumber(m.group(6));
                        } catch (IllegalArgumentException x) {
                            // OK, some other kind of dep, just ignore
                            continue;
                        }
                    } else { //m2.matches()
                        groupId = m2.group(1);
                        artifactId = m2.group(2);
                        try {
                            version = new VersionNumber(m2.group(6));
                        } catch (IllegalArgumentException x) {
                            // OK, some other kind of dep, just ignore
                            continue;
                        }
                    }

                    if (groupId.equals("org.jenkins-ci.main") && artifactId.equals("jenkins-core")) {
                        coreDep = version;
                    } else if (groupId.equals("org.jenkins-ci.plugins")) {
                        if(m2.matches()) {
                            pluginDepsTest.put(artifactId, version);
                        } else {
                            pluginDeps.put(artifactId, version);
                        }
                    } else if (groupId.equals("org.jenkins-ci.main") && artifactId.equals("maven-plugin")) {
                        if(m2.matches()) {
                            pluginDepsTest.put(artifactId, version);
                        } else {
                            pluginDeps.put(artifactId, version);
                        }
                    } else if (groupId.equals(pluginGroupIds.get(artifactId))) {
                        if(m2.matches()) {
                            pluginDepsTest.put(artifactId, version);
                        } else {
                            pluginDeps.put(artifactId, version);
                        }
                    }
                }
            }
        } finally {
            Files.delete(tmp.toPath());
        }
        System.out.println("Analysis: coreDep=" + coreDep + " pluginDeps=" + pluginDeps + " pluginDepsTest=" + pluginDepsTest);
        if (coreDep != null) {
            Map<String,VersionNumber> toAdd = new HashMap<>();
            Map<String,VersionNumber> toReplace = new HashMap<>();
            Map<String,VersionNumber> toAddTest = new HashMap<>();
            Map<String,VersionNumber> toReplaceTest = new HashMap<>();
            overridenPlugins.forEach(plugin -> {
                toReplace.put(plugin.getName(), plugin.getVersion());
                toReplaceTest.put(plugin.getName(), plugin.getVersion());
                if (plugin.getGroupId() != null) {
                    if (pluginGroupIds.containsKey(plugin.getName())) {
                        if (!plugin.getGroupId().equals(pluginGroupIds.get(plugin.getName()))) {
                            System.err.println("WARNING: mismatch between detected and explicit group ID for " + plugin.getName());
                        }
                    } else {
                        pluginGroupIds.put(plugin.getName(), plugin.getGroupId());
                    }
                }
            });

            for (String split : splits) {
                String[] pieces = split.split(" ");
                String plugin = pieces[0];
                if (splitCycles.contains(thisPlugin + ' ' + plugin)) {
                    System.out.println("Skipping implicit dep " + thisPlugin + " → " + plugin);
                    continue;
                }
                VersionNumber splitPoint = new VersionNumber(pieces[1]);
                VersionNumber declaredMinimum = new VersionNumber(pieces[2]);
                if (coreDep.compareTo(splitPoint) < 0 && new VersionNumber(coreVersion).compareTo(splitPoint) >=0 && !pluginDeps.containsKey(plugin)) {
                    Plugin bundledP = otherPlugins.get(plugin);
                    if (bundledP != null) {
                        VersionNumber bundledV;
                        try {
                            bundledV = new VersionNumber(bundledP.version);
                        } catch (NumberFormatException x) { // TODO apparently this does not handle `1.0-beta-1` and the like?!
                            System.out.println("Skipping unparseable dep on " + bundledP.name + ": " + bundledP.version);
                            continue;
                        }
                        if (bundledV.isNewerThan(declaredMinimum)) {
                            toAdd.put(plugin, bundledV);
                            continue;
                        }
                    }
                    toAdd.put(plugin, declaredMinimum);
                }
            }

            List<String> convertFromTestDep = new ArrayList<>();
            checkDefinedDeps(pluginDeps, toAdd, toReplace, otherPlugins, new ArrayList<>(pluginDepsTest.keySet()), convertFromTestDep);
            pluginDepsTest.putAll(difference(pluginDepsTest, toAdd));
            pluginDepsTest.putAll(difference(pluginDepsTest, toReplace));
            checkDefinedDeps(pluginDepsTest, toAddTest, toReplaceTest, otherPlugins);

            // Could contain transitive dependencies which were part of the plugin's dependencies or to be added
            toAddTest = difference(pluginDeps, toAddTest);
            toAddTest = difference(toAdd, toAddTest);

            if (!toAdd.isEmpty() || !toReplace.isEmpty() || !toAddTest.isEmpty() || !toReplaceTest.isEmpty()) {
                System.out.println("Adding/replacing plugin dependencies for compatibility: " + toAdd + " " + toReplace + "\nFor test: " + toAddTest + " " + toReplaceTest);
                pom.addDependencies(toAdd, toReplace, toAddTest, toReplaceTest, coreDep, pluginGroupIds, convertFromTestDep);
            }

        // TODO(oleg_nenashev): This is a hack, logic above should be refactored somehow (JENKINS-55279)
            // Remove the self-dependency if any
            pom.removeDependency(pluginGroupIds.get(thisPlugin), thisPlugin);
        }
    }

    private void checkDefinedDeps(Map<String,VersionNumber> pluginList, Map<String,VersionNumber> adding, Map<String,VersionNumber> replacing, Map<String,Plugin> otherPlugins) {
        checkDefinedDeps(pluginList, adding, replacing, otherPlugins, new ArrayList<>(), null);
    }
    private void checkDefinedDeps(Map<String,VersionNumber> pluginList, Map<String,VersionNumber> adding, Map<String,VersionNumber> replacing, Map<String,Plugin> otherPlugins, List<String> inTest, List<String> toConvertFromTest) {
        for (Map.Entry<String,VersionNumber> pluginDep : pluginList.entrySet()) {
            String plugin = pluginDep.getKey();
            Plugin bundledP = otherPlugins.get(plugin);
            if (bundledP != null) {
                VersionNumber bundledV = new VersionNumber(bundledP.version);
                if (bundledV.isNewerThan(pluginDep.getValue())) {
                    assert !adding.containsKey(plugin);
                    replacing.put(plugin, bundledV);
                }
                // Also check any dependencies, so if we are upgrading cloudbees-folder, we also add an explicit dep on a bundled credentials.
                for (Map.Entry<String,String> dependency : bundledP.dependencies.entrySet()) {
                    String depPlugin = dependency.getKey();
                    if (pluginList.containsKey(depPlugin)) {
                        continue; // already handled
                    }
                    // We ignore the declared dependency version and go with the bundled version:
                    Plugin depBundledP = otherPlugins.get(depPlugin);
                    if (depBundledP != null) {
                        updateAllDependents(plugin, depBundledP, pluginList, adding, replacing, otherPlugins, inTest, toConvertFromTest);
                    }
                }
            }
        }
    }

    /**
     * Search the dependents of a given plugin to determine if we need to use the bundled version.
     * This helps in cases where tests fail due to new insufficient versions as well as more
     * accurately representing the totality of upgraded plugins for provided war files.
     */
    private void updateAllDependents(String parent, Plugin dependent, Map<String,VersionNumber> pluginList, Map<String,VersionNumber> adding, Map<String,VersionNumber> replacing, Map<String,Plugin> otherPlugins, List<String> inTest, List<String> toConvertFromTest) {
        // Check if this exists with an undesired scope
        String pluginName = dependent.name;
        if (inTest.contains(pluginName)) {
            // This is now required in the compile scope.  For example: copyartifact's dependency matrix-project requires junit
            System.out.println("Converting " + pluginName + " from the test scope since it was a dependency of " + parent);
            toConvertFromTest.add(pluginName);
            replacing.put(pluginName, new VersionNumber(dependent.version));
        } else {
            System.out.println("Adding " + pluginName + " since it was a dependency of " + parent);
            adding.put(pluginName, new VersionNumber(dependent.version));
        }
        // Also check any dependencies
        for (Map.Entry<String,String> dependency : dependent.dependencies.entrySet()) {
            String depPlugin = dependency.getKey();
            if (pluginList.containsKey(depPlugin)) {
                continue; // already handled
            }

            // We ignore the declared dependency version and go with the bundled version:
            Plugin depBundledP = otherPlugins.get(depPlugin);
            if (depBundledP != null) {
                updateAllDependents(pluginName, depBundledP, pluginList, adding, replacing, otherPlugins, inTest, toConvertFromTest);
            }
        }
    }

    /** Use JENKINS-47634 to load metadata from jenkins-core.jar if available. */
    private void populateSplits(File war) throws IOException {
        System.out.println("Checking " + war + " for plugin split metadata…");
        System.out.println("Checking jdk version as splits may depend on a jdk version");
        JavaSpecificationVersion jdkVersion = new JavaSpecificationVersion(config.getTestJavaVersion()); // From Java 9 onwards there is a standard for versions see JEP-223
        try (JarFile jf = new JarFile(war, false)) {
            Enumeration<JarEntry> warEntries = jf.entries();
            while (warEntries.hasMoreElements()) {
                JarEntry coreJar = warEntries.nextElement();
                if (coreJar.getName().matches("WEB-INF/lib/jenkins-core-.+[.]jar")) {
                    try (InputStream is = jf.getInputStream(coreJar);
                         JarInputStream jis = new JarInputStream(is, false)) {
                        JarEntry entry;
                        int found = 0;
                        while ((entry = jis.getNextJarEntry()) != null) {
                            if (entry.getName().equals("jenkins/split-plugins.txt")) {
                                splits = configLines(jis).collect(Collectors.toList());
                                // Since https://github.com/jenkinsci/jenkins/pull/3865 splits can depend on jdk version
                                // So make sure we are not applying splits not intended for our JDK
                                splits = removeSplitsBasedOnJDK(splits, jdkVersion);
                                System.out.println("found splits: " + splits);
                                found++;
                            } else if (entry.getName().equals("jenkins/split-plugin-cycles.txt")) {
                                splitCycles = configLines(jis).collect(Collectors.toSet());
                                System.out.println("found split cycles: " + splitCycles);
                                found++;
                            }
                        }
                        if (found == 0) {
                            System.out.println("None found, falling back to hard-coded historical values.");
                            splits = HISTORICAL_SPLITS;
                            splitCycles = HISTORICAL_SPLIT_CYCLES;
                        } else if (found != 2) {
                            throw new IOException("unexpected amount of metadata");
                        }
                    }
                    return;
                }
            }
        }
        throw new IOException("no jenkins-core-*.jar found in " + war);
    }

    private List<String> removeSplitsBasedOnJDK(List<String> splits, JavaSpecificationVersion jdkVersion) {
        List<String> filterSplits = new LinkedList<>();
        for (String split : splits) {
            String[] tokens = split.trim().split("\\s+");
            if (tokens.length == 4 ) { // We have a jdk field in the splits file
                if (jdkVersion.isNewerThanOrEqualTo(new JavaSpecificationVersion(tokens[3]))) {
                    filterSplits.add(split);
                } else {
                    System.out.println("Not adding " + split + " as split because jdk specified " + tokens[3] + " is newer than running jdk " + jdkVersion);
                }
            } else {
                filterSplits.add(split);
            }
        }
        return filterSplits;
    }

    // Matches syntax in ClassicPluginStrategy:
    private static Stream<String> configLines(InputStream is) throws IOException {
        return IOUtils.readLines(is, StandardCharsets.UTF_8).stream().filter(line -> !line.matches("#.*|\\s*"));
    }
    private static final ImmutableList<String> HISTORICAL_SPLITS = ImmutableList.of(
        "maven-plugin 1.296 1.296",
        "subversion 1.310 1.0",
        "cvs 1.340 0.1",
        "ant 1.430 1.0",
        "javadoc 1.430 1.0",
        "external-monitor-job 1.467 1.0",
        "ldap 1.467 1.0",
        "pam-auth 1.467 1.0",
        "mailer 1.493 1.2",
        "matrix-auth 1.535 1.0.2",
        "windows-slaves 1.547 1.0",
        "antisamy-markup-formatter 1.553 1.0",
        "matrix-project 1.561 1.0",
        "junit 1.577 1.0",
        "bouncycastle-api 2.16 2.16.0",
        "command-launcher 2.86 1.0"
    );
    private static final ImmutableSet<String> HISTORICAL_SPLIT_CYCLES = ImmutableSet.of(
        "script-security matrix-auth",
        "script-security windows-slaves",
        "script-security antisamy-markup-formatter",
        "script-security matrix-project",
        "script-security bouncycastle-api",
        "script-security command-launcher",
        "credentials matrix-auth",
        "credentials windows-slaves"
    );

    /**
     * Finds the difference of the given maps.
     * In set theory: base - toAdd
     *
     * @param base the left map; all returned items are not in this map
     * @param toAdd the right map; all returned items are found in this map
     */
    private Map<String, VersionNumber> difference(Map<String, VersionNumber> base, Map<String, VersionNumber> toAdd) {
        Map<String, VersionNumber> diff = new HashMap<>();
        for (Map.Entry<String,VersionNumber> adding : toAdd.entrySet()) {
            if (!base.containsKey(adding.getKey())) {
                diff.put(adding.getKey(), adding.getValue());
            }
        }
        return diff;
    }
}