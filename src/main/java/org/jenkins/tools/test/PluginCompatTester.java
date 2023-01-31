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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.UpdateSite;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.exception.ExecutedTestNamesSolverException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.model.MavenBom;
import org.jenkins.tools.test.maven.MavenRunner;
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
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCompile;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHooks;
import org.jenkins.tools.test.util.ExecutedTestNamesSolver;
import org.jenkins.tools.test.util.StreamGobbler;
import org.jenkins.tools.test.exception.PomTransformationException;

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
            throws IOException, PomExecutionException, XmlPullParserException {
        PluginCompatTesterHooks pcth = new PluginCompatTesterHooks(config.getHookPrefixes(), config.getExternalHooksJars(), config.getExcludeHooks());
        // Providing XSL Stylesheet along xml report file
        if(config.reportFile != null){
            if(config.isProvideXslReport()){
                Files.createDirectories(Paths.get(PluginCompatReport.getBaseFilepath(config.reportFile)));
                File xslFilePath = PluginCompatReport.getXslFilepath(config.reportFile);
                try (InputStream is = getXslTransformerResource()) {
                    Files.copy(is, xslFilePath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        // Determine the plugin data
        HashMap<String,String> pluginGroupIds = new HashMap<>();  // Used to track real plugin groupIds from WARs

        // Scan bundled plugins
        // If there is any bundled plugin, only these plugins will be taken under the consideration for the PCT run
        UpdateSite.Data data;
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

        final Map<String, UpdateSite.Plugin> pluginsToCheck;
        final List<String> pluginsToInclude = config.getIncludePlugins();
        if (data.plugins.isEmpty() && pluginsToInclude != null && !pluginsToInclude.isEmpty()) {
            // Update Center returns empty info OR the "-war" option is specified for WAR without bundled plugins
            LOGGER.log(Level.INFO, "WAR file does not contain plugin info; will try to extract it from UC for included plugins");
            pluginsToCheck = new HashMap<>(pluginsToInclude.size());
            UpdateSite.Data ucData = extractUpdateCenterData(pluginGroupIds);
            for (String plugin : pluginsToInclude) {
                UpdateSite.Plugin pluginData = ucData.plugins.get(plugin);
                if (pluginData != null) {
                    LOGGER.log(Level.INFO, "Adding {0} to the test scope", plugin);
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
                UpdateSite.Plugin extracted = extractFromLocalCheckout();
                pluginsToCheck.put(artifactId, extracted);
            } catch (PluginSourcesUnavailableException e) {
                LOGGER.log(Level.SEVERE, "Cannot test {0} because plugin sources are not available despite a local checkout being provided", artifactId);
            }
        }


        PluginCompatReport report = PluginCompatReport.fromXml(config.reportFile);

        SortedSet<MavenCoordinates> testedCores = config.getWar() == null ? generateCoreCoordinatesToTest(data, report) : coreVersionFromWAR(data);

        MavenRunner.Config mconfig = new MavenRunner.Config(config);
        // TODO REMOVE
        mconfig.userProperties.put( "failIfNoTests", "false" );
        report.setTestJavaVersion(config.getTestJavaVersion());

        boolean failed = false;
        ROOT_CYCLE: for(MavenCoordinates coreCoordinates : testedCores){
            LOGGER.log(Level.INFO, "Starting plugin tests on core coordinates {0}", coreCoordinates);
            for (UpdateSite.Plugin plugin : pluginsToCheck.values()) {
                if(config.getIncludePlugins()==null || config.getIncludePlugins().contains(plugin.name.toLowerCase())){
                    PluginInfos pluginInfos = new PluginInfos(plugin.name, plugin.version, plugin.url);

                    if(config.getExcludePlugins()!=null && config.getExcludePlugins().contains(plugin.name.toLowerCase())){
                        LOGGER.log(Level.INFO, "Plugin {0} is in excluded plugins => test skipped", plugin.name);
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
                    } catch (Throwable t) {
                        status = TestStatus.INTERNAL_ERROR;
                        LOGGER.log(Level.SEVERE, String.format("Internal error while executing a test for core %s and plugin %s %s. Please submit a bug to plugin-compat-tester",
                                coreCoordinates.version, plugin.getDisplayName(), plugin.version), t);
                        errorMessage = t.getMessage();
                        pomData = null;
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
                            FileUtils.touch(config.reportFile);
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
                    LOGGER.log(Level.FINE, "Plugin {0} not in included plugins; skipping", plugin.name);
                }
            }
        }

        // Generating HTML report only if needed, if the file does not exist is because no test has been executed
        if(config.isGenerateHtmlReport() && config.reportFile != null && config.reportFile.exists()) {
            generateHtmlReportFile();
        } else {
            LOGGER.log(Level.INFO, "No HTML report has been generated, either because report generation has been disabled or because no tests have been executed");
        }

        if (failed && config.isFailOnError()) {
            throw new RuntimeException("Execution was aborted due to the failure in a plugin test (-failOnError is set)");
        }

        return report;
    }

    private UpdateSite.Plugin extractFromLocalCheckout() throws PluginSourcesUnavailableException {
        PomData data = new PluginRemoting(new File(config.getLocalCheckoutDir(), "pom.xml")).retrievePomData();
        JSONObject o = new JSONObject();
        o.put("name", data.artifactId);
        o.put("version", ""); // version is not required
        o.put("url", data.getConnectionUrl());
        o.put("dependencies", new JSONArray());
        return new UpdateSite.Plugin(DEFAULT_SOURCE_ID, o);
    }

    protected void generateHtmlReportFile() throws IOException {
        if (!config.reportFile.exists() || !config.reportFile.isFile()) {
            throw new FileNotFoundException("Cannot find the XML report file: " + config.reportFile);
        }

        Source xmlSource = new StreamSource(config.reportFile);
        try(InputStream xsltStream = getXslTransformerResource()) {
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

    private static InputStream getXslTransformerResource(){
        return PluginCompatTester.class.getResourceAsStream("resultToReport.xsl");
    }

    private static File createBuildLogFile(File reportFile, String pluginName, String pluginVersion, MavenCoordinates coreCoords){
        return new File(reportFile.getParentFile().getAbsolutePath()
                            + File.separator + createBuildLogFilePathFor(pluginName, pluginVersion, coreCoords));
    }

    private static String createBuildLogFilePathFor(String pluginName, String pluginVersion, MavenCoordinates coreCoords){
        return String.format("logs/%s/v%s_against_%s_%s_%s.log", pluginName, pluginVersion, coreCoords.groupId, coreCoords.artifactId, coreCoords.version);
    }

    private TestExecutionResult testPluginAgainst(MavenCoordinates coreCoordinates, UpdateSite.Plugin plugin, MavenRunner.Config mconfig, PomData pomData, PluginCompatTesterHooks pcth)
            throws PluginSourcesUnavailableException, PomExecutionException, IOException, PomTransformationException {
        LOGGER.log(Level.INFO, "\n\n\n\n\n\n#############################################\n#############################################\n##\n## Starting to test {0} {1} against {2}\n##\n#############################################\n#############################################\n\n\n\n\n", new Object[]{plugin.name, plugin.version, coreCoordinates});

        File pluginCheckoutDir = new File(config.workDirectory.getAbsolutePath() + File.separator + plugin.name + File.separator);
        String parentFolder = "";

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
                    LOGGER.log(Level.INFO, "Deleting working directory {0}", pluginCheckoutDir.getAbsolutePath());
                    FileUtils.deleteDirectory(pluginCheckoutDir);
                }

                Files.createDirectory(pluginCheckoutDir.toPath());
                LOGGER.log(Level.INFO, "Created plugin checkout directory {0}", pluginCheckoutDir.getAbsolutePath());

                if (localCheckoutProvided()) {
                    if (!onlyOnePluginIncluded()) {
                        File localCheckoutPluginDir = new File(config.getLocalCheckoutDir(), plugin.name);
                        File pomLocalCheckoutPluginDir = new File(localCheckoutPluginDir, "pom.xml");
                        if(pomLocalCheckoutPluginDir.exists()) {
                            LOGGER.log(Level.INFO, "Copying plugin directory from {0}", localCheckoutPluginDir.getAbsolutePath());
                            org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(localCheckoutPluginDir, pluginCheckoutDir);
                        } else {
                            cloneFromSCM(pomData, plugin.name, plugin.version, pluginCheckoutDir, "");
                        }
                    } else {
                        // TODO this fails when it encounters symlinks (e.g. work/jobs/…/builds/lastUnstableBuild),
                        // and even up-to-date versions of org.apache.commons.io.FileUtils seem to not handle links,
                        // so may need to use something like http://docs.oracle.com/javase/tutorial/displayCode.html?code=http://docs.oracle.com/javase/tutorial/essential/io/examples/Copy.java
                        LOGGER.log(Level.INFO, "Copy plugin directory from {0}", config.getLocalCheckoutDir().getAbsolutePath());
                        org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(config.getLocalCheckoutDir(), pluginCheckoutDir);
                    }
                } else {
                    // These hooks could redirect the SCM, skip checkout (if multiple plugins use the same preloaded repo)
                    cloneFromSCM(pomData, plugin.name, plugin.version, pluginCheckoutDir, "");
                }
            } else {
                // If the plugin exists in a different directory (multi-module plugins)
                if (beforeCheckout.get("pluginDir") != null) {
                    pluginCheckoutDir = (File)beforeCheckout.get("checkoutDir");
                }
                if (beforeCheckout.get("parentFolder") != null) {
                    parentFolder = (String) beforeCheckout.get("parentFolder");
                }
                LOGGER.log(Level.INFO, "The plugin has already been checked out, likely due to a multi-module situation; continuing");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check out plugin sources", e);
            throw new PluginSourcesUnavailableException("Failed to check out plugin sources", e);
        }

        File buildLogFile = createBuildLogFile(config.reportFile, plugin.name, plugin.version, coreCoordinates);
        FileUtils.forceMkdir(buildLogFile.getParentFile()); // Creating log directory
        FileUtils.touch(buildLogFile); // Creating log file

        // Ran the BeforeCompileHooks
        Map<String, Object> beforeCompile = new HashMap<>();
        beforeCompile.put("pluginName", plugin.name);
        beforeCompile.put("plugin", plugin);
        beforeCompile.put("pluginDir", pluginCheckoutDir);
        beforeCompile.put("pomData", pomData);
        beforeCompile.put("config", config);
        beforeCompile.put("core", coreCoordinates);
        if (parentFolder != null && !parentFolder.isEmpty()) {
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
            args.add(String.format("--define=forkCount=%s", userProperties.getOrDefault("forkCount", "1")));
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
            forExecutionHooks.put("pom", new MavenPom(pluginCheckoutDir));
            forExecutionHooks.put("coreCoordinates", coreCoordinates);
            forExecutionHooks.put("config", config);
            forExecutionHooks.put("pluginDir", pluginCheckoutDir);
            forExecutionHooks.put("types", testTypes);
            pcth.runBeforeExecution(forExecutionHooks);
            args = (List<String>)forExecutionHooks.get("args");
            Set<String> types = new HashSet<>((List<String>) forExecutionHooks.get("types"));
            userProperties.put("types", String.join(",", types));

            // Execute with tests
            runner.run(mconfig, pluginCheckoutDir, buildLogFile, args.toArray(new String[0]));
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

    public void cloneFromSCM(PomData pomData, String name, String version, File checkoutDirectory, String tag) throws IOException {
	String scmTag = !(tag.equals("")) ? tag : getScmTag(pomData, name, version);
        String connectionURLPomData = pomData.getConnectionUrl();
        List<String> connectionURLs = new ArrayList<>();
        connectionURLs.add(connectionURLPomData);
        if(config.getFallbackGitHubOrganization() != null){
            connectionURLs = getFallbackConnectionURL(connectionURLs, connectionURLPomData, config.getFallbackGitHubOrganization());
        }

        IOException lastException = null;
        for (String connectionURL: connectionURLs){
            if (connectionURL != null) {
                connectionURL = connectionURL.replace("git://", "https://"); // See: https://github.blog/2021-09-01-improving-git-protocol-security-github/
            }
            try {
                clone(connectionURL, scmTag, checkoutDirectory);
                break;
            } catch (IOException e) {
                if (lastException != null) {
                    e.addSuppressed(lastException);
                }
                lastException = e;
            }
        }

        if (lastException != null) {
            throw new UncheckedIOException(lastException);
        }
    }

    /**
     * Clone the given Git repository in the given checkout directory by running, in order, the
     * following CLI operations:
     *
     * <ul>
     *   <li><code>git init</code>
     *   <li><code>git remote add origin url</code>
     *   <li><code>git fetch origin ${SCM_TAG}</code>
     *   <li><code>git checkout FETCH_HEAD</code>
     * </ul>
     *
     * @param connectionURL The connection URL, in a format such as
     *     scm:git:https://github.com/jenkinsci/mailer-plugin.git or
     *     https://github.com/jenkinsci/mailer-plugin.git
     * @param scmTag the tag or sha1 hash to clone
     * @param checkoutDirectory the directory in which to clone the Git repository
     * @throws IOException if an error occurs
     */
    public static void clone(String connectionURL, String scmTag, File checkoutDirectory)
            throws IOException {
        LOGGER.log(Level.INFO, "Checking out from SCM connection URL {0} at {1}", new Object[]{connectionURL, scmTag});

        /*
         * We previously used the Maven SCM API to clone the repository, which ran the following
         * commands:
         *
         *     git clone --depth 1 --branch ${SCM_TAG} ${CONNECTION_URL}
         *     git ls-remote ${CONNECTION_URL}
         *     git fetch ${CONNECTION_URL}
         *     git checkout ${SCM_TAG}
         *     git ls-files
         *
         * This proved to be inefficient, so we instead run only the commands we need to run:
         *
         *     git init
         *     git fetch ${CONNECTION_URL} ${SCM_TAG} (this will work with a SHA1 hash or a tag)
         *     git checkout FETCH_HEAD
         */
        if (checkoutDirectory.isDirectory()) {
            FileUtils.deleteDirectory(checkoutDirectory);
        }
        Files.createDirectories(checkoutDirectory.toPath());

        // git init
        Process p = new ProcessBuilder()
                .directory(checkoutDirectory)
                .command("git", "init")
                .redirectErrorStream(true)
                .start();
        StreamGobbler gobbler = new StreamGobbler(p.getInputStream());
        gobbler.start();
        try {
            int exitStatus = p.waitFor();
            gobbler.join();
            String output = gobbler.getOutput().trim();
            if (exitStatus != 0) {
                throw new IOException("git init failed with exit status " + exitStatus + ": " + output);
            }
        } catch (InterruptedException e) {
            throw new IOException("git init was interrupted", e);
        }

        // git fetch ${CONNECTION_URL} ${SCM_TAG}
        String gitUrl;
        if (StringUtils.startsWith(connectionURL, "scm:git:")) {
            gitUrl = StringUtils.substringAfter(connectionURL, "scm:git:");
        } else {
            gitUrl = connectionURL;
        }
        p = new ProcessBuilder()
                .directory(checkoutDirectory)
                .command("git", "fetch", gitUrl, scmTag)
                .redirectErrorStream(true)
                .start();
        gobbler = new StreamGobbler(p.getInputStream());
        gobbler.start();
        try {
            int exitStatus = p.waitFor();
            gobbler.join();
            String output = gobbler.getOutput().trim();
            if (exitStatus != 0) {
                throw new IOException("git fetch origin failed with exit status " + exitStatus + ": " + output);
            }
        } catch (InterruptedException e) {
            throw new IOException("git fetch origin was interrupted", e);
        }

        // git checkout FETCH_HEAD
        p = new ProcessBuilder()
                .directory(checkoutDirectory)
                .command("git", "checkout", "FETCH_HEAD")
                .redirectErrorStream(true)
                .start();
        gobbler = new StreamGobbler(p.getInputStream());
        gobbler.start();
        try {
            int exitStatus = p.waitFor();
            gobbler.join();
            String output = gobbler.getOutput().trim();
            if (exitStatus != 0) {
                throw new IOException("git checkout FETCH_HEAD failed with exit status " + exitStatus + ": " + output);
            }
        } catch (InterruptedException e) {
            throw new IOException("git checkout FETCH_HEAD was interrupted", e);
        }
    }

    private String getScmTag(PomData pomData, String name, String version){
        String scmTag;
        if (pomData.getScmTag() != null) {
            scmTag = pomData.getScmTag();
            LOGGER.log(Level.INFO, "Using SCM tag {0} from POM", scmTag);
        } else {
            scmTag = name + "-" + version;
            LOGGER.log(Level.INFO, "POM did not provide an SCM tag; inferring tag {0}", scmTag);
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
        try {
            url = new URL(config.updateCenterUrl);
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }

        String jsonp;
        try (InputStream is = url.openStream()) {
            jsonp = new String(is.readAllBytes(), Charset.defaultCharset());
        }catch(IOException e){
            throw new UncheckedIOException("Invalid Update Center URL: " + config.updateCenterUrl, e);
        }

        String json = jsonp.substring(jsonp.indexOf('(')+1,jsonp.lastIndexOf(')'));

        JSONObject jsonObj = JSONObject.fromObject(json);
        UpdateSite.Data site = new UpdateSite.Data(jsonObj);

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
    		        LOGGER.log(Level.INFO, "Entry {0} defined in the BOM looks unparseable; continuing", name);
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
    		if (core == null || core.isEmpty()) {
    			throw new IllegalStateException("Unable to retrieve any version for the core");
    		}
    		top.put("core", new JSONObject().accumulate("name", "core").accumulate("version",core).accumulate("url", "https://foobar"));
    	}
        LOGGER.log(Level.INFO, "Read contents of {0}: {1}", new Object[]{config.getBom(), top});
        return new UpdateSite.Data(top);
    }

    private List<File> getBomEntries() throws IOException, XmlPullParserException, PomExecutionException {
        File fullDepPom = new MavenBom(config.getBom()).writeFullDepPom(config.workDirectory);

        MavenRunner.Config mconfig = new MavenRunner.Config(config);
    	LOGGER.log(Level.INFO, "User settings file: {0}", mconfig.userSettingsFile);

    	File buildLogFile = new File(config.workDirectory
    			+ File.separator + "bom-download.log");
    	FileUtils.touch(buildLogFile); // Creating log file

    	runner.run(mconfig, fullDepPom.getParentFile(), buildLogFile, "dependency:copy-dependencies", "-P consume-incrementals", "-N");
    	return org.codehaus.plexus.util.FileUtils.getFiles(new File(config.workDirectory, "bom" + File.separator + "target" + File.separator + "dependency"),null, null);
    }

    /**
     * @return Provides the core version from the bomfile and in case it is not found, the bom version, if bom version does not exist, it provides from its parent
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
	return version != null ? version : parent != null ? parent.getVersion() : "";
    }

    /**
     * Given a value and a model, it checks if it is an interpolated value. In negative case it returns the same
     * value. In affirmative case, it retrieves its value from the properties of the Maven model.
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
    private UpdateSite.Data scanWAR(File war, @NonNull Map<String, String> pluginGroupIds, String pluginRegExp) throws IOException {
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
        LOGGER.log(Level.INFO, "Scanned contents of {0} with {1} plugins", new Object[]{war, plugins.size()});
        return new UpdateSite.Data(top);
    }

    private SortedSet<MavenCoordinates> coreVersionFromWAR(UpdateSite.Data data) {
        SortedSet<MavenCoordinates> result = new TreeSet<>();
        result.add(new MavenCoordinates(PluginCompatTesterConfig.DEFAULT_PARENT_GROUP, PluginCompatTesterConfig.DEFAULT_PARENT_ARTIFACT, data.core.version));
        return result;
    }

    /**
     * Provides the Maven module used for a plugin on a {@code mvn [...] -pl} operation in the parent path 
     */
    public static String getMavenModule(String plugin, File pluginPath, MavenRunner runner, MavenRunner.Config mavenConfig) throws PomExecutionException, IOException {
        String absolutePath = pluginPath.getAbsolutePath();
        if (absolutePath.endsWith(plugin)) {
            return plugin;
        }
        String module = absolutePath.substring(absolutePath.lastIndexOf(File.separatorChar) + 1);
        File parentFile = pluginPath.getParentFile();
        if (parentFile == null) {
            return null;
        }
        File log = new File(parentFile.getAbsolutePath() + File.separatorChar + "modules.log");
        runner.run(mavenConfig, parentFile, log, "-Dexpression=project.modules", "-q", "-DforceStdout", "help:evaluate");
        for (String line : Files.readAllLines(log.toPath(), Charset.defaultCharset())) {
            if (!StringUtils.startsWith(line.trim(), "<string>")) {
                continue;
            }
            String mvnModule = line.replace("<string>", "").replace("</string>", "").trim();
            if (mvnModule != null && mvnModule.contains(module)) {
                return mvnModule;
            }
        }
        return null;
    }
}
