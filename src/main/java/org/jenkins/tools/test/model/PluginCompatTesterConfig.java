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
package org.jenkins.tools.test.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * POJO used to configure Plugin Compatibility Tester execution
 *
 * @author Frederic Camblor
 */
public class PluginCompatTesterConfig {

    private static final Logger LOGGER = Logger.getLogger(PluginCompatTesterConfig.class.getName());

    public static final String DEFAULT_PARENT_GROUP = "org.jenkins-ci.plugins";
    public static final String DEFAULT_PARENT_ARTIFACT = "plugin";

    // Update center used to retrieve plugins informations
    public String updateCenterUrl = "https://updates.jenkins.io/current/update-center.json";

    // A working directory where the tested plugin's sources will be checked out
    public File workDirectory;

    // A report file where will be generated testing report
    // If the file already exist, testing report will be merged into it
    public File reportFile;

    // Path for maven settings file where repository will be provided allowing to
    // download jenkins-core artifact (and dependencies)
    private File m2SettingsFile;

    // GroupId which will be used to replace tested plugin's parent groupId
    // If null, every recorded core coordinates (in report xml) will be played
    private String parentGroupId = DEFAULT_PARENT_GROUP;
    // ArtifactId which will be used to replace tested plugin's parent artifactId
    // If null, every recorded core coordinates (in report xml) will be played
    private String parentArtifactId = DEFAULT_PARENT_ARTIFACT;
    // Version which will be used to replace tested plugin's parent version
    // If null, latest core version (retrieved via the update center) will be used
    private String parentVersion = null;

    private File war = null;

    /**
     * A Java HOME to be used for running tests in plugins.
     */
    @CheckForNull
    private File testJDKHome = null;

    @CheckForNull
    private String testJavaArgs = null;

    @CheckForNull
    private File externalMaven = null;

    // List of plugin artifact ids on which tests will be performed
    // If null, tests will be performed on every plugins retrieved from update center
    private List<String> includePlugins = null;

    // List of plugin artifact ids on which tests will be not performed
    // If null, tests will be performed on every includePlugins found
    private List<String> excludePlugins = null;

    // List of hooks that will not be executed
    // If null, all hooks will be executed
    private List<String> excludeHooks = null;

    // URL to be used as an alternative to download plugin source from fallback
    // organtizations, like your own fork
    private String fallbackGitHubOrganization = null;

    // Allows to skip a plugin test if this plugin test has already been performed
    // within testCacheTimeout ms
    private long testCacheTimeout = 1000L * 60 * 60 * 24 * 100;
    // Skips test cache : plugin will be tested, no matter the test cache is
    private boolean skipTestCache = false;
    // Allows to define a minimal cache threshold for TestStatus
    // That is to say, every results lower than this threshold won't be put
    // into the cache
    private TestStatus cacheThresholdStatus = TestStatus.COMPILATION_ERROR;

    // Allows to provide XSL report file near XML report file
    // Only if reportFile is not null
    private boolean provideXslReport = true;

    // Allows to generate HTML Report file
    // Only if reportFile is not null
    private boolean generateHtmlReport = true;

    private Map<String, String> mavenProperties = Collections.emptyMap();
    private String mavenPropertiesFile;

    private List<String> mavenOptions = Collections.emptyList();
 
    // Classpath prefixes of the extra hooks
    private List<String> hookPrefixes = new ArrayList<>(Collections.singletonList("org.jenkins"));
    
    // External hooks jar files path locations
    private List<File> externalHooksJars = new ArrayList<>();

    // Path for a folder containing a local (possibly modified) clone of a plugin repository
    private File localCheckoutDir;

    private List<PCTPlugin> overridenPlugins = new ArrayList<>();

    // Immediately if the PCT run fails for a plugin. Error status will be also reported as a return code
    private boolean failOnError;

    // Path to a BOM file to get plugin data
    private File bom;

    // Flag to indicate if we want to store all the tests names or only failed ones on PCT report files
    private boolean storeAll;

    /**
     * @deprecated just for tests; use {@link #PluginCompatTesterConfig()} and call whatever setters are actually required
     */
    @Deprecated
    public PluginCompatTesterConfig(File workDirectory, File reportFile, File m2SettingsFile){
        setWorkDirectory(workDirectory);
        setReportFile(reportFile);
        setM2SettingsFile(m2SettingsFile);
    }

    public PluginCompatTesterConfig() {}

    public void setUpdateCenterUrl(String updateCenterUrl) {
        this.updateCenterUrl = updateCenterUrl;
    }

    public void setWorkDirectory(File workDirectory) {
        this.workDirectory = workDirectory;
    }

    public void setReportFile(File reportFile) {
        this.reportFile = reportFile;
    }

    public void setM2SettingsFile(File m2SettingsFile) {
        this.m2SettingsFile = m2SettingsFile;
    }

    public void setParentGroupId(String parentGroupId) {
        this.parentGroupId = parentGroupId;
    }

    public void setParentArtifactId(String parentArtifactId) {
        this.parentArtifactId = parentArtifactId;
    }

    public String getParentVersion() {
        return parentVersion;
    }

    public void setParentVersion(String parentVersion) {
        this.parentVersion = parentVersion;
    }

    public List<String> getIncludePlugins() {
        return includePlugins;
    }

    public void setIncludePlugins(List<String> pluginsList) {
        this.includePlugins = pluginsList;
    }

    public File getM2SettingsFile() {
        return m2SettingsFile;
    }

    public long getTestCacheTimeout() {
        return testCacheTimeout;
    }

    public void setTestCacheTimeout(long testCacheTimeout) {
        this.testCacheTimeout = testCacheTimeout;
    }

    public boolean isSkipTestCache() {
        return skipTestCache;
    }

    public void setSkipTestCache(boolean skipTestCache) {
        this.skipTestCache = skipTestCache;
    }

    public boolean isProvideXslReport() {
        return provideXslReport;
    }

    public void setProvideXslReport(boolean provideXslReport) {
        this.provideXslReport = provideXslReport;
    }

    public boolean isGenerateHtmlReport() {
        return generateHtmlReport;
    }

    public void setGenerateHtmlReport(boolean generateHtmlReport) {
        this.generateHtmlReport = generateHtmlReport;
    }

    public String getParentGroupId() {
        return parentGroupId;
    }

    @CheckForNull
    public File getTestJDKHome() {
        return testJDKHome;
    }

    @CheckForNull
    public String getTestJavaArgs() {
        return testJavaArgs;
    }

    public String getParentArtifactId() {
        return parentArtifactId;
    }

    public List<String> getExcludePlugins() {
        return excludePlugins;
    }

    public void setExcludePlugins(List<String> excludePlugins) {
        this.excludePlugins = excludePlugins;
    }

    public List<String> getExcludeHooks() {
        return excludeHooks;
    }

    public void setExcludeHooks(List<String> excludeHooks) {
        this.excludeHooks = excludeHooks;
    }

    public String getFallbackGitHubOrganization() {
        return fallbackGitHubOrganization;
    }

    public void setFallbackGitHubOrganization(String fallbackGitHubOrganization) {
        this.fallbackGitHubOrganization = fallbackGitHubOrganization;
    }

    public void setMavenProperties(@Nonnull Map<String, String> mavenProperties) {
        this.mavenProperties = new HashMap<>(mavenProperties);
    }

    /**
     * Gets a list of Maven properties defined in the configuration. It is not a full list of
     * properties; {@link #retrieveMavenProperties()} should be used to construct it.
     */
    @Nonnull
    public Map<String, String> getMavenProperties() {
        return Collections.unmodifiableMap(mavenProperties);
    }

    public String getMavenPropertiesFile() {
        return mavenPropertiesFile;
    }

    public void setMavenPropertiesFiles( String mavenPropertiesFile ) {
        this.mavenPropertiesFile = mavenPropertiesFile;
    }

    public List<String> getMavenOptions() {
        return mavenOptions;
    }

    public void setMavenOptions(List<String> mavenOptions) {
        this.mavenOptions = mavenOptions;
    }

    @CheckForNull
    public File getBom() {
        return bom;
    }

    public void setBom(File bom) {
        this.bom = bom;
    }

    /**
     * Retrieves Maven Properties from available sources like {@link #mavenPropertiesFile}.
     *
     * @return Map of properties
     * @throws IOException Property read failure
     * @since TODO
     */
    public Map<String, String> retrieveMavenProperties() throws IOException {
        Map<String, String> res = new HashMap<>(mavenProperties);

        // Read properties from File
        if ( StringUtils.isNotBlank( mavenPropertiesFile )) {
            File file = new File (mavenPropertiesFile);
            if (file.exists() && file.isFile()) {
                try(FileInputStream fileInputStream = new FileInputStream(file)) {
                    Properties properties = new Properties(  );
                    properties.load( fileInputStream  );
                    for (Map.Entry<Object,Object> entry : properties.entrySet()) {
                        res.put((String) entry.getKey(), (String) entry.getValue());
                    }
                }
            } else {
                throw new IOException("Extra Maven Properties File " + mavenPropertiesFile + " does not exist or not a File" );
            }
        }

        // Read other explicit CLI arguments

        // Override JDK if passed explicitly
        if (testJDKHome != null) {
            if (!testJDKHome.exists() || !testJDKHome.isDirectory()) {
                throw new IOException("Wrong Test JDK Home passed as a parameter: " + testJDKHome);
            }

            if (res.containsKey("jvm")) {
                LOGGER.log(Level.WARNING, "Maven properties already contain the 'jvm' argument; " +
                        "overriding the previous test JDK home value '" + res.get("jvm") +
                        "' by the explicit argument: " + testJDKHome);
            } else {
                LOGGER.log(Level.INFO, "Using custom test JDK home: {0}", testJDKHome);
            }
            final String javaCmdAbsolutePath = getTestJavaCommandPath();
            res.put("jvm", javaCmdAbsolutePath);
        }

        // Merge test Java args if needed
        if (StringUtils.isNotBlank(testJavaArgs)) {
            if (res.containsKey("argLine")) {
                LOGGER.log(Level.WARNING, "Maven properties already contain the 'argLine' argument; " +
                        "merging value from properties and from the command line");
                res.put("argLine", res.get("argLine") + " " + testJavaArgs);
            } else {
                res.put("argLine", testJavaArgs);
            }
        }

        return res;
    }

        @CheckForNull
    private String getTestJavaCommandPath() {
        if(testJDKHome==null) {
            return null;
        }
        return new File(testJDKHome, "bin/java").getAbsolutePath();
    }

    /**
     * Gets the Java version used for testing, using the binary path to the <code>java</code>
     * command.
     *
     * @return a string identifying the jvm in use
     */
    public String getTestJavaVersion() throws IOException {
        String javaCmdAbsolutePath = getTestJavaCommandPath();
        if (javaCmdAbsolutePath == null) {
            LOGGER.log(Level.INFO, "Test JDK home unset; using Java from PATH");
            javaCmdAbsolutePath = "java";
        }
        final Process process = new ProcessBuilder().command(javaCmdAbsolutePath, "-XshowSettings:properties -version").redirectErrorStream(true).start();
        final String javaVersionOutput = IOUtils.toString(process.getInputStream());
        final String[] lines = javaVersionOutput.split("[\\r\\n]+");
        for (String line: lines) {
            String trimmed = line.trim();
            if (trimmed.contains("java.specification.version")) {
                //java.specification.version = version
                return trimmed.split("=")[1].trim();
            }
        }
        // Default to fullversion output as before
        final Process process2 = new ProcessBuilder().command(javaCmdAbsolutePath, "-fullversion").redirectErrorStream(true).start();
        final String javaVersionOutput2 = IOUtils.toString(process2.getInputStream());
        // Expected format is something like openjdk full version "1.8.0_181-8u181-b13-2~deb9u1-b13"
        // We shorten it by removing the "full version" in the middle
        return javaVersionOutput2.
                replace(" full version ", " ").
                replaceAll("\"", "");
    }

    public TestStatus getCacheThresholdStatus() {
        return cacheThresholdStatus;
    }

    public void setCacheThresholdStatus(TestStatus cacheThresholdStatus) {
        this.cacheThresholdStatus = cacheThresholdStatus;
    }

    public File getWar() {
        return war;
    }

    public void setWar(File war) {
        this.war = war;
    }

    @CheckForNull
    public File getExternalMaven() {
        return externalMaven;
    }

    public void setExternalMaven(File externalMaven) {
        this.externalMaven = externalMaven;
    }

    public List<String> getHookPrefixes() {
        return hookPrefixes;
    }
    
    public List<File> getExternalHooksJars() {
        return externalHooksJars;
    }

    public void setHookPrefixes(List<String> hookPrefixes) {
        // Want to also process the default
        this.hookPrefixes.addAll(hookPrefixes);
    }
    
    public void setExternalHooksJars(List<File> externalHooksJars) {
        this.externalHooksJars = externalHooksJars;
    }

    /**
     * Sets JDK Home for tests
     *
     * @param testJDKHome JDK home to be used. {@code null} for using default system one.
     */
    public void setTestJDKHome(@CheckForNull File testJDKHome) {
        this.testJDKHome = testJDKHome;
    }

    public void setTestJavaArgs(@CheckForNull String testJavaArgs) {
        this.testJavaArgs = testJavaArgs;
    }

    public File getLocalCheckoutDir() {
        return localCheckoutDir;
    }

    public void setLocalCheckoutDir(String localCheckoutDir) {
        this.localCheckoutDir = new File(localCheckoutDir);
    }

    public void setOverridenPlugins(List<PCTPlugin> overridenPlugins) {
        this.overridenPlugins = overridenPlugins;
    }

    public List<PCTPlugin> getOverridenPlugins() {
        return overridenPlugins;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public void setStoreAll(boolean storeAll) {
        this.storeAll = storeAll;
    }
    
    public boolean isStoreAll() {
        return storeAll;
    }
}
