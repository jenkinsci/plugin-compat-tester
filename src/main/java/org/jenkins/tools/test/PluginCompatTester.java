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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.model.UpdateSite;
import hudson.model.UpdateSite.Plugin;
import hudson.util.VersionNumber;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
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
    public static final String JENKINS_CORE_FILE_REGEX = "WEB-INF/lib/jenkins-core-([0-9.]+(?:-[0-9a-f.]+)*(?:-(?i)([a-z]+)(-)?([0-9a-f.]+)?)?(?:-(?i)([a-z]+)(-)?([0-9a-f_.]+)?)?(?:-SNAPSHOT)?)[.]jar";

    private PluginCompatTesterConfig config;
    private final ExternalMavenRunner runner;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "not mutated after this point I hope")
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
        PluginCompatTesterHooks pcth = new PluginCompatTesterHooks(config.getHookPrefixes(), config.getExternalHooksJars(), config.getExcludeHooks());
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

        // if there is only one plugin and it's not already resolved (not in the war, not in a bom and not in an update center)
        // and there is a local checkout available then it needs to be added to the plugins to check
        if (onlyOnePluginIncluded() && localCheckoutProvided() && !pluginsToCheck.containsKey(config.getIncludePlugins().get(0))) {
            String artifactId = config.getIncludePlugins().get(0);
            try {
                Plugin extracted = extractFromLocalCheckout();
                pluginsToCheck.put(artifactId, extracted);
            } catch (PluginSourcesUnavailableException e) {
                LOGGER.log(Level.SEVERE, String.format("Local checkout provided but plugin sources are not available. Cannot test plugin [%s]", artifactId));
            }
        }


        PluginCompatReport report = PluginCompatReport.fromXml(config.reportFile);

        SortedSet<MavenCoordinates> testedCores = config.getWar() == null ? generateCoreCoordinatesToTest(data, report) : coreVersionFromWAR(data);

        MavenRunner.Config mconfig = new MavenRunner.Config(config);
        // TODO REMOVE
        mconfig.userProperties.put( "failIfNoTests", "false" );
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
                        TestExecutionResult result = testPluginAgainst(actualCoreCoordinates, plugin, mconfig, pomData, pcth);
                        if (result.getTestDetails().isSuccess()) {
                            status = TestStatus.SUCCESS;
                        } else {
                            status = TestStatus.TEST_FAILURES;
                        }
                        warningMessages.addAll(result.pomWarningMessages);
                        testDetails.addAll(config.isStoreAll() ? result.getTestDetails().getAll() : result.getTestDetails().hasFailures() ? result.getTestDetails().getFailed() : Collections.emptySet());
                    } catch (PomExecutionException e) {
                        if(!e.succeededPluginArtifactIds.contains("maven-compiler-plugin")){
                            status = TestStatus.COMPILATION_ERROR;
                        } else if (!e.getTestDetails().hasBeenExecuted()) { // testing was not able to start properly (i.e: invalid exclusion list file format)
                            status = TestStatus.INTERNAL_ERROR;
                        } else if (e.getTestDetails().hasFailures()) { 
                            status = TestStatus.TEST_FAILURES;                            
                        } else { // ???
                            status = TestStatus.INTERNAL_ERROR;
                        }
                        errorMessage = e.getErrorMessage();
                        warningMessages.addAll(e.getPomWarningMessages());
                        testDetails.addAll(config.isStoreAll() ? e.getTestDetails().getAll() : e.getTestDetails().hasFailures() ? e.getTestDetails().getFailed() : Collections.emptySet());
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

    private Plugin extractFromLocalCheckout() throws PluginSourcesUnavailableException {
        PomData data = new PluginRemoting(new File(config.getLocalCheckoutDir(), "pom.xml")).retrievePomData();
        JSONObject o = new JSONObject();
        o.put("name", data.artifactId);
        o.put("version", ""); // version is not required
        o.put("url", data.getConnectionUrl());
        o.put("dependencies", new JSONArray());
        return new UpdateSite(DEFAULT_SOURCE_ID, null).new Plugin(DEFAULT_SOURCE_ID, o);
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

    private TestExecutionResult testPluginAgainst(MavenCoordinates coreCoordinates, Plugin plugin, MavenRunner.Config mconfig, PomData pomData, PluginCompatTesterHooks pcth)
            throws PluginSourcesUnavailableException, PomExecutionException, IOException {
        System.out.println(String.format("%n%n%n%n%n"));
        System.out.println("#############################################");
        System.out.println("#############################################");
        System.out.println(String.format("##%n## Starting to test plugin %s v%s%n## against %s%n##", plugin.name, plugin.version, coreCoordinates));
        System.out.println("#############################################");
        System.out.println("#############################################");
        System.out.println(String.format("%n%n%n%n%n"));

        File pluginCheckoutDir = new File(config.workDirectory.getAbsolutePath() + File.separator + plugin.name + File.separator);
        String parentFolder = StringUtils.EMPTY;

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
                            cloneFromSCM(pomData, plugin.name, plugin.version, pluginCheckoutDir, "");
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
                    cloneFromSCM(pomData, plugin.name, plugin.version, pluginCheckoutDir, "");
                }
            } else {
                // If the plugin exists in a different directory (multimodule plugins)
                if (beforeCheckout.get("pluginDir") != null) {
                    pluginCheckoutDir = (File)beforeCheckout.get("checkoutDir");
                }
                if (beforeCheckout.get("parentFolder") != null) {
                    parentFolder = (String) beforeCheckout.get("parentFolder");
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
        if (StringUtils.isNotEmpty(parentFolder)) {
            beforeCompile.put("parentFolder", parentFolder);
        }
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

            List<String> args = new ArrayList<>();
            Map<String, String> userProperties = mconfig.userProperties;
            args.add(String.format("--define=forkCount=%s",userProperties.containsKey("forkCount") ? userProperties.get("forkCount") : "1"));
            args.add("hpi:resolve-test-dependencies");
            args.add("hpi:test-hpl");
            args.add("surefire:test");

            // Run preexecution hooks
            List<String> testTypes = new LinkedList<>();
            testTypes.add("surefire"); // default
            Map<String, Object> forExecutionHooks = new HashMap<>();
            forExecutionHooks.put("pluginName", plugin.name);
            forExecutionHooks.put("plugin", plugin);
            forExecutionHooks.put("args", args);
            forExecutionHooks.put("pomData", pomData);
            forExecutionHooks.put("coreCoordinates", coreCoordinates);
            forExecutionHooks.put("config", config);
            forExecutionHooks.put("pluginDir", pluginCheckoutDir);
            forExecutionHooks.put("types", testTypes);
            pcth.runBeforeExecution(forExecutionHooks);
            args = (List<String>)forExecutionHooks.get("args");
            Set<String> types = new HashSet<>((List<String>) forExecutionHooks.get("types"));
            userProperties.put("types", String.join(",", types));

            // Execute with tests
            runner.run(mconfig, pluginCheckoutDir, buildLogFile, args.toArray(new String[args.size()]));
            return new TestExecutionResult(((PomData)forExecutionHooks.get("pomData")).getWarningMessages(), new ExecutedTestNamesSolver().solve(types, runner.getExecutedTests(), pluginCheckoutDir));
        } catch (ExecutedTestNamesSolverException e) {
            throw new PomExecutionException(e);
        } catch (PomExecutionException e){
            e.getPomWarningMessages().addAll(pomData.getWarningMessages());
            if (ranCompile) {
                // So the status cannot be considered COMPILATION_ERROR
                e.succeededPluginArtifactIds.add("maven-compiler-plugin");
            }
            throw e;
        }
    }

    public void cloneFromSCM(PomData pomData, String name, String version, File checkoutDirectory, String tag) throws ComponentLookupException, ScmException, IOException {
	String scmTag = !(tag.equals("")) ? tag : getScmTag(pomData, name, version);
        String connectionURLPomData = pomData.getConnectionUrl();
        List<String> connectionURLs = new ArrayList<String>();
        connectionURLs.add(connectionURLPomData);
        if(config.getFallbackGitHubOrganization() != null){
            connectionURLs = getFallbackConnectionURL(connectionURLs, connectionURLPomData, config.getFallbackGitHubOrganization());
        }

        Boolean repositoryCloned = false;
        String errorMessage = "";
        ScmRepository repository;
        ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
        for (String connectionURL: connectionURLs){
            if (connectionURL != null) {
                connectionURL = connectionURL.replace("git://", "https://"); // See: https://github.blog/2021-09-01-improving-git-protocol-security-github/
            }
            System.out.println("Checking out from SCM connection URL : " + connectionURL + " (" + name + "-" + version + ") at tag " + scmTag);
            if (checkoutDirectory.isDirectory()) {
                FileUtils.deleteDirectory(checkoutDirectory);
            }
            repository = scmManager.makeScmRepository(connectionURL);
            CheckOutScmResult result = scmManager.checkOut(repository, new ScmFileSet(checkoutDirectory), new ScmTag(scmTag));
            if(result.isSuccess()){
                repositoryCloned = true;
                break;
            } else {
                errorMessage = result.getProviderMessage() + " || " + result.getCommandOutput();
            }
        }

        if (!repositoryCloned) {
            throw new RuntimeException(errorMessage);
        }
    }

    private String getScmTag(PomData pomData, String name, String version){
        String scmTag;
        if (pomData.getScmTag() != null) {
            scmTag = pomData.getScmTag();
            System.out.println(String.format("Using SCM tag '%s' from POM.", scmTag));
        } else {
            scmTag = name + "-" + version;
            System.out.println(String.format("POM did not provide an SCM tag. Inferring tag '%s'.", scmTag));
        }
        return scmTag;
    }

    public static List<String> getFallbackConnectionURL(List<String> connectionURLs,String connectionURLPomData, String fallbackGitHubOrganization){
        Pattern pattern = Pattern.compile("(.*github.com[:|/])([^/]*)(.*)");
        Matcher matcher = pattern.matcher(connectionURLPomData);
        matcher.find();
        connectionURLs.add(matcher.replaceFirst("scm:git:git@github.com:" + fallbackGitHubOrganization + "$3"));
        pattern = Pattern.compile("(.*github.com[:|/])([^/]*)(.*)");
        matcher = pattern.matcher(connectionURLPomData);
        matcher.find();
        connectionURLs.add(matcher.replaceFirst("$1" + fallbackGitHubOrganization + "$3"));
        return connectionURLs;
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

        MavenRunner.Config mconfig = new MavenRunner.Config(config);
    	System.out.println(mconfig.userSettingsFile);

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
        System.out.println("Scanned contents of " + war + " with " + plugins.size() + " plugins");
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
    
    /**
     * Provides the Maven module used for a plugin on a {@code mvn [...] -pl} operation in the parent path 
     */
    public static String getMavenModule(String plugin, File pluginPath, MavenRunner runner, MavenRunner.Config mavenConfig) throws PomExecutionException, IOException {
        String absolutePath = pluginPath.getAbsolutePath();
        if (absolutePath.endsWith(plugin)) {
            return plugin;
        }
        String module = absolutePath.substring(absolutePath.lastIndexOf(File.separatorChar) + 1, absolutePath.length());
        File parentFile = pluginPath.getParentFile();
        if (parentFile == null) {
            return null;
        }
        File log = new File(parentFile.getAbsolutePath() + File.separatorChar + "modules.log");
        runner.run(mavenConfig, parentFile, log, "-Dexpression=project.modules", "-q", "-DforceStdout", "help:evaluate");
        for (String line : org.apache.commons.io.FileUtils.readLines(log)) {
            if (!StringUtils.startsWith(line.trim(), "<string>")) {
                continue;
            }
            String mvnModule = line.replace("<string>", "").replace("</string>", "").trim();
            if (StringUtils.contains(mvnModule, module)) {
                return mvnModule;
            }
        }
        return null;
    }
}
