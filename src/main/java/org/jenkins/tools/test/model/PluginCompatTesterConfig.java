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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * POJO used to configure Plugin Compatibility Tester execution
 *
 * @author Frederic Camblor
 */
public class PluginCompatTesterConfig {

    // The megawar
    @NonNull
    private final File war;

    // A working directory where the tested plugin's sources will be checked out
    @NonNull
    private final File workingDir;

    // List of plugin artifact ids on which tests will be performed
    // If empty, tests will be performed on every plugins retrieved from update center
    @NonNull
    private Set<String> includePlugins = Set.of();

    // List of plugin artifact ids on which tests will be not performed
    // If empty, tests will be performed on every includePlugins found
    @NonNull
    private Set<String> excludePlugins = Set.of();

    // List of hooks that will not be executed
    // If empty, all hooks will be executed
    @NonNull
    private Set<String> excludeHooks = Set.of();

    // URL to be used as an alternative to download plugin source from fallback
    // organizations, like your own fork
    @CheckForNull
    private String fallbackGitHubOrganization;

    @CheckForNull
    private File externalMaven;

    // Path for maven settings file where repository will be provided allowing to
    // download jenkins-core artifact (and dependencies)
    @CheckForNull
    private File mavenSettings;

    @NonNull
    private Map<String, String> mavenProperties = Map.of();

    @NonNull
    private List<String> mavenArgs = List.of();

    // External hooks jar files path locations
    @NonNull
    private Set<File> externalHooksJars = Set.of();

    // Path for a folder containing a local (possibly modified) clone of a plugin repository
    @CheckForNull
    private File localCheckoutDir;

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
    public Set<String> getIncludePlugins() {
        return includePlugins;
    }

    public void setIncludePlugins(@NonNull Set<String> includePlugins) {
        this.includePlugins = Set.copyOf(includePlugins);
    }

    @NonNull
    public Set<String> getExcludePlugins() {
        return excludePlugins;
    }

    public void setExcludePlugins(@NonNull Set<String> excludePlugins) {
        this.excludePlugins = Set.copyOf(excludePlugins);
    }

    @NonNull
    public Set<String> getExcludeHooks() {
        return excludeHooks;
    }

    public void setExcludeHooks(@NonNull Set<String> excludeHooks) {
        this.excludeHooks = Set.copyOf(excludeHooks);
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
    public Set<File> getExternalHooksJars() {
        return externalHooksJars;
    }

    public void setExternalHooksJars(@NonNull Set<File> externalHooksJars) {
        this.externalHooksJars = Set.copyOf(externalHooksJars);
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
