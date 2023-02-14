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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * POJO used to configure Plugin Compatibility Tester execution
 *
 * @author Frederic Camblor
 */
public class PluginCompatTesterConfig {

    // The megawar
    @NonNull private final File war;

    // A working directory where the tested plugin's sources will be checked out
    @NonNull private final File workingDir;

    // List of plugin artifact ids on which tests will be performed
    // If empty, tests will be performed on every plugins retrieved from update center
    @NonNull private List<String> includePlugins = List.of();

    // List of plugin artifact ids on which tests will be not performed
    // If empty, tests will be performed on every includePlugins found
    @NonNull private List<String> excludePlugins = List.of();

    // List of hooks that will not be executed
    // If empty, all hooks will be executed
    @NonNull private List<String> excludeHooks = List.of();

    // URL to be used as an alternative to download plugin source from fallback
    // organizations, like your own fork
    @CheckForNull private String fallbackGitHubOrganization;

    @CheckForNull private File externalMaven;

    // Path for maven settings file where repository will be provided allowing to
    // download jenkins-core artifact (and dependencies)
    @CheckForNull private File mavenSettings;

    @NonNull private Map<String, String> mavenProperties = Map.of();

    @NonNull private List<String> mavenArgs = List.of();

    // Classpath prefixes of the extra hooks
    @NonNull private List<String> hookPrefixes = List.of("org.jenkins");

    // External hooks jar files path locations
    @NonNull private List<File> externalHooksJars = List.of();

    // Path for a folder containing a local (possibly modified) clone of a plugin repository
    @CheckForNull private File localCheckoutDir;

    // If multiple plugins are specified, fail the overall run after the first plugin failure occurs
    // rather than continuing to test other plugins.
    private boolean failFast;

    public PluginCompatTesterConfig(@NonNull File war, @NonNull File workingDir) {
        this.war = war;
        this.workingDir = workingDir;
    }

    @NonNull
    public File getWar() {
        return war;
    }

    @NonNull
    public File getWorkingDir() {
        return workingDir;
    }

    @NonNull
    public List<String> getIncludePlugins() {
        return includePlugins;
    }

    public void setIncludePlugins(@NonNull List<String> includePlugins) {
        this.includePlugins = List.copyOf(includePlugins);
    }

    @NonNull
    public List<String> getExcludePlugins() {
        return excludePlugins;
    }

    public void setExcludePlugins(@NonNull List<String> excludePlugins) {
        this.excludePlugins = List.copyOf(excludePlugins);
    }

    @NonNull
    public List<String> getExcludeHooks() {
        return excludeHooks;
    }

    public void setExcludeHooks(@NonNull List<String> excludeHooks) {
        this.excludeHooks = List.copyOf(excludeHooks);
    }

    @CheckForNull
    public String getFallbackGitHubOrganization() {
        return fallbackGitHubOrganization;
    }

    public void setFallbackGitHubOrganization(@CheckForNull String fallbackGitHubOrganization) {
        this.fallbackGitHubOrganization = fallbackGitHubOrganization;
    }

    @CheckForNull
    public File getExternalMaven() {
        return externalMaven;
    }

    public void setExternalMaven(@CheckForNull File externalMaven) {
        this.externalMaven = externalMaven;
    }

    @CheckForNull
    public File getMavenSettings() {
        return mavenSettings;
    }

    public void setMavenSettings(@CheckForNull File mavenSettings) {
        this.mavenSettings = mavenSettings;
    }

    @NonNull
    public Map<String, String> getMavenProperties() {
        return mavenProperties;
    }

    public void setMavenProperties(@NonNull Map<String, String> mavenProperties) {
        this.mavenProperties = Collections.unmodifiableMap(mavenProperties);
    }

    @NonNull
    public List<String> getMavenArgs() {
        return mavenArgs;
    }

    public void setMavenArgs(@NonNull List<String> mavenArgs) {
        this.mavenArgs = List.copyOf(mavenArgs);
    }

    @NonNull
    public List<String> getHookPrefixes() {
        return hookPrefixes;
    }

    public void setHookPrefixes(@NonNull List<String> hookPrefixes) {
        // Want to also process the default
        List<String> combined = new ArrayList<>();
        combined.addAll(this.hookPrefixes);
        combined.addAll(hookPrefixes);
        this.hookPrefixes = List.copyOf(combined);
    }

    @NonNull
    public List<File> getExternalHooksJars() {
        return externalHooksJars;
    }

    public void setExternalHooksJars(@NonNull List<File> externalHooksJars) {
        this.externalHooksJars = List.copyOf(externalHooksJars);
    }

    @CheckForNull
    public File getLocalCheckoutDir() {
        return localCheckoutDir;
    }

    public void setLocalCheckoutDir(@CheckForNull File localCheckoutDir) {
        this.localCheckoutDir = localCheckoutDir;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }
}
