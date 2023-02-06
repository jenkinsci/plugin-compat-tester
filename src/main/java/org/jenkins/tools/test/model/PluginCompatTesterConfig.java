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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins.tools.test.util.StreamGobbler;

/**
 * POJO used to configure Plugin Compatibility Tester execution
 *
 * @author Frederic Camblor
 */
public class PluginCompatTesterConfig {

    private static final Logger LOGGER = Logger.getLogger(PluginCompatTesterConfig.class.getName());

    // A working directory where the tested plugin's sources will be checked out
    public File workDirectory;

    // A report file where will be generated testing report
    // If the file already exist, testing report will be merged into it
    public File reportFile;

    // Path for maven settings file where repository will be provided allowing to
    // download jenkins-core artifact (and dependencies)
    private File m2SettingsFile;

    // The megawar
    private File war;

    /** A Java HOME to be used for running tests in plugins. */
    @CheckForNull private File testJDKHome;

    @CheckForNull private File externalMaven;

    // List of plugin artifact ids on which tests will be performed
    // If null, tests will be performed on every plugins retrieved from update center
    private List<String> includePlugins;

    // List of plugin artifact ids on which tests will be not performed
    // If null, tests will be performed on every includePlugins found
    private List<String> excludePlugins;

    // List of hooks that will not be executed
    // If null, all hooks will be executed
    private List<String> excludeHooks;

    // URL to be used as an alternative to download plugin source from fallback
    // organtizations, like your own fork
    private String fallbackGitHubOrganization;

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
    private List<String> hookPrefixes = new ArrayList<>(List.of("org.jenkins"));

    // External hooks jar files path locations
    private List<File> externalHooksJars = new ArrayList<>();

    // Path for a folder containing a local (possibly modified) clone of a plugin repository
    private File localCheckoutDir;

    // Immediately if the PCT run fails for a plugin. Error status will be also reported as a return
    // code
    private boolean failOnError;

    // Flag to indicate if we want to store all the tests names or only failed ones on PCT report
    // files
    private boolean storeAll;

    /**
     * @deprecated just for tests; use {@link #PluginCompatTesterConfig()} and call whatever setters
     *     are actually required
     */
    @Deprecated
    public PluginCompatTesterConfig(File workDirectory, File reportFile, File m2SettingsFile) {
        setWorkDirectory(workDirectory);
        setReportFile(reportFile);
        setM2SettingsFile(m2SettingsFile);
    }

    public PluginCompatTesterConfig() {}

    public void setWorkDirectory(File workDirectory) {
        this.workDirectory = workDirectory;
    }

    public void setReportFile(File reportFile) {
        this.reportFile = reportFile;
    }

    public void setM2SettingsFile(File m2SettingsFile) {
        this.m2SettingsFile = m2SettingsFile;
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

    @CheckForNull
    public File getTestJDKHome() {
        return testJDKHome;
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

    public void setMavenProperties(@NonNull Map<String, String> mavenProperties) {
        this.mavenProperties = new HashMap<>(mavenProperties);
    }

    /**
     * Gets a list of Maven properties defined in the configuration. It is not a full list of
     * properties; {@link #retrieveMavenProperties()} should be used to construct it.
     */
    @NonNull
    public Map<String, String> getMavenProperties() {
        return Collections.unmodifiableMap(mavenProperties);
    }

    public String getMavenPropertiesFile() {
        return mavenPropertiesFile;
    }

    public void setMavenPropertiesFiles(String mavenPropertiesFile) {
        this.mavenPropertiesFile = mavenPropertiesFile;
    }

    public List<String> getMavenOptions() {
        return mavenOptions;
    }

    public void setMavenOptions(List<String> mavenOptions) {
        this.mavenOptions = mavenOptions;
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
        if (mavenPropertiesFile != null && !mavenPropertiesFile.isBlank()) {
            File file = new File(mavenPropertiesFile);
            if (file.exists() && file.isFile()) {
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    Properties properties = new Properties();
                    properties.load(fileInputStream);
                    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                        res.put((String) entry.getKey(), (String) entry.getValue());
                    }
                }
            } else {
                throw new FileNotFoundException(
                        "Extra Maven Properties File "
                                + mavenPropertiesFile
                                + " does not exist or not a File");
            }
        }

        // Read other explicit CLI arguments

        // Override JDK if passed explicitly
        if (testJDKHome != null) {
            if (!testJDKHome.exists() || !testJDKHome.isDirectory()) {
                throw new IOException("Wrong Test JDK Home passed as a parameter: " + testJDKHome);
            }

            if (res.containsKey("jvm")) {
                LOGGER.log(
                        Level.WARNING,
                        "Maven properties already contain the 'jvm' argument; "
                                + "overriding the previous test JDK home value '"
                                + res.get("jvm")
                                + "' by the explicit argument: "
                                + testJDKHome);
            } else {
                LOGGER.log(Level.INFO, "Using custom test JDK home: {0}", testJDKHome);
            }
            final String javaCmdAbsolutePath = getTestJavaCommandPath();
            res.put("jvm", javaCmdAbsolutePath);
        }

        return res;
    }

    @CheckForNull
    private String getTestJavaCommandPath() {
        if (testJDKHome == null) {
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
        final Process process =
                new ProcessBuilder()
                        .command(javaCmdAbsolutePath, "-XshowSettings:properties", "-version")
                        .redirectErrorStream(true)
                        .start();
        StreamGobbler gobbler = new StreamGobbler(process.getInputStream());
        gobbler.start();
        try {
            int exitStatus = process.waitFor();
            gobbler.join();
            if (exitStatus != 0) {
                throw new IOException(
                        "java -XshowSettings:properties -version failed with exit status "
                                + exitStatus
                                + ": "
                                + gobbler.getOutput().trim());
            }
        } catch (InterruptedException e) {
            throw new IOException("interrupted while getting Java version", e);
        }
        final String javaVersionOutput = gobbler.getOutput().trim();
        final String[] lines = javaVersionOutput.split("[\\r\\n]+");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("java.specification.version")) {
                // java.specification.version = version
                return trimmed.split("=")[1].trim();
            }
        }
        // Default to fullversion output as before
        final Process process2 =
                new ProcessBuilder()
                        .command(javaCmdAbsolutePath, "-fullversion")
                        .redirectErrorStream(true)
                        .start();
        StreamGobbler gobbler2 = new StreamGobbler(process2.getInputStream());
        gobbler2.start();
        try {
            int exitStatus2 = process2.waitFor();
            gobbler2.join();
            if (exitStatus2 != 0) {
                throw new IOException(
                        "java -fullversion failed with exit status "
                                + exitStatus2
                                + ": "
                                + gobbler2.getOutput().trim());
            }
        } catch (InterruptedException e) {
            throw new IOException("interrupted while getting full Java version", e);
        }
        final String javaVersionOutput2 = gobbler2.getOutput().trim();
        // Expected format is something like openjdk full version "1.8.0_181-8u181-b13-2~deb9u1-b13"
        // We shorten it by removing the "full version" in the middle
        return javaVersionOutput2.replace(" full version ", " ").replaceAll("\"", "");
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

    public File getLocalCheckoutDir() {
        return localCheckoutDir;
    }

    public void setLocalCheckoutDir(String localCheckoutDir) {
        this.localCheckoutDir = new File(localCheckoutDir);
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
