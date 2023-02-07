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
import java.util.logging.Logger;

/**
 * POJO used to configure Plugin Compatibility Tester execution
 *
 * @author Frederic Camblor
 */
public class PluginCompatTesterConfig {

    private static final Logger LOGGER = Logger.getLogger(PluginCompatTesterConfig.class.getName());

    // A working directory where the tested plugin's sources will be checked out
    public File workDirectory;

    // Path for maven settings file where repository will be provided allowing to
    // download jenkins-core artifact (and dependencies)
    private File m2SettingsFile;

    // The megawar
    private File war;

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

    private Map<String, String> mavenProperties = Collections.emptyMap();
    private String mavenPropertiesFile;

    private List<String> mavenArgs = Collections.emptyList();

    // Classpath prefixes of the extra hooks
    private List<String> hookPrefixes = new ArrayList<>(List.of("org.jenkins"));

    // External hooks jar files path locations
    private List<File> externalHooksJars = new ArrayList<>();

    // Path for a folder containing a local (possibly modified) clone of a plugin repository
    private File localCheckoutDir;

    // If multiple plugins are specified, fail the overall run after the first plugin failure occurs
    // rather than continuing to test other plugins.
    private boolean failFast;

    /**
     * @deprecated just for tests; use {@link #PluginCompatTesterConfig()} and call whatever setters
     *     are actually required
     */
    @Deprecated
    public PluginCompatTesterConfig(File workDirectory, File m2SettingsFile) {
        setWorkDirectory(workDirectory);
        setM2SettingsFile(m2SettingsFile);
    }

    public PluginCompatTesterConfig() {}

    public void setWorkDirectory(File workDirectory) {
        this.workDirectory = workDirectory;
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

    public List<String> getMavenArgs() {
        return mavenArgs;
    }

    public void setMavenArgs(List<String> mavenArgs) {
        this.mavenArgs = mavenArgs;
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

        return res;
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

    public File getLocalCheckoutDir() {
        return localCheckoutDir;
    }

    public void setLocalCheckoutDir(String localCheckoutDir) {
        this.localCheckoutDir = new File(localCheckoutDir);
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }
}
