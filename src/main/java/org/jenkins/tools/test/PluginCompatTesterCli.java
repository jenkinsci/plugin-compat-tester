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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.logging.LoggingConfiguration;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.picocli.ExistingFileTypeConverter;
import picocli.CommandLine;

@CommandLine.Command(
        name = "test-plugins",
        mixinStandardHelpOptions = true,
        description =
                "Perform a compatibility test for plugins against Jenkins core and other plugins.",
        versionProvider = VersionProvider.class)
public class PluginCompatTesterCli implements Callable<Integer> {

    static {
        String configFile = System.getProperty("java.util.logging.config.file");
        String configClass = System.getProperty("java.util.logging.config.class");
        if (configClass == null && configFile == null) {
            new LoggingConfiguration();
        }
    }

    @CommandLine.Option(
            names = {"-w", "--war"},
            required = true,
            description = "Path to the WAR file to be used by the PCT.",
            converter = ExistingFileTypeConverter.class)
    private File war;

    @CommandLine.Option(
            names = "--working-dir",
            required = true,
            description = "Working directory where plugin sources will be checked out")
    private File workingDir;

    @CheckForNull
    @CommandLine.Option(
            names = "--include-plugins",
            split = ",",
            arity = "1",
            paramLabel = "plugin",
            description =
                    "Comma-separated list of plugin artifact IDs to test. If not set, every plugin in the WAR will be tested.")
    private List<String> includePlugins;

    @CheckForNull
    @CommandLine.Option(
            names = "--exclude-plugins",
            split = ",",
            arity = "1",
            paramLabel = "plugin",
            description =
                    "Comma-separated list of plugin artifact IDs to skip. If not set, only the plugins specified by --plugins will be tested (or all plugins otherwise).")
    private List<String> excludePlugins;

    @CheckForNull
    @CommandLine.Option(
            names = "--exclude-hooks",
            split = ",",
            arity = "1",
            paramLabel = "hook",
            description =
                    "Comma-separated list of hooks to skip. If not set, all hooks will be executed.")
    private List<String> excludeHooks;

    @CheckForNull
    @CommandLine.Option(
            names = "--fallback-github-organization",
            description =
                    "Include an alternative organization to use as a fallback to download the plugin. It is useful for using your own fork releases for a specific plugin if the version is not found in the official repository. If set, The PCT will try to use the fallback if a plugin tag is not found at the regular URL.")
    private String fallbackGitHubOrganization;

    @CheckForNull
    @CommandLine.Option(
            names = "--mvn",
            description = "The path to the Maven executable.",
            converter = ExistingFileTypeConverter.class)
    private File externalMaven;

    @CheckForNull
    @CommandLine.Option(
            names = "--maven-settings",
            description = "Settings file to use when executing Maven.",
            converter = ExistingFileTypeConverter.class)
    private File mavenSettings;

    @CheckForNull
    @CommandLine.Option(
            names = {"-D", "--define"},
            description =
                    "Define a user property to be passed to Maven when running tests. Note that these will NOT be passed to Maven during compilation.")
    private Map<String, String> mavenProperties;

    @CheckForNull
    @CommandLine.Option(
            names = "--maven-args",
            split = ",",
            arity = "1",
            paramLabel = "arg",
            description =
                    "Comma-separated list of arguments to pass to Maven (like -Pxxx; not to be confused with Java arguments or Maven properties). These arguments will be passed to Maven both during compilation and when running tests.")
    private List<String> mavenArgs;

    @CheckForNull
    @CommandLine.Option(
            names = "--external-hooks-jars",
            split = ",",
            arity = "1",
            paramLabel = "jar",
            description = "Comma-separated list of paths to external hooks JARs.",
            converter = ExistingFileTypeConverter.class)
    private List<File> externalHooksJars;

    @CheckForNull
    @CommandLine.Option(
            names = "--local-checkout-dir",
            description =
                    "Folder containing either a local (possibly modified) clone of a plugin repository or a set of local clones of different plugins.",
            converter = ExistingFileTypeConverter.class)
    private File localCheckoutDir;

    @CommandLine.Option(
            names = "--fail-fast",
            negatable = true,
            description =
                    "If multiple plugins are specified, fail the overall run after the first plugin failure occurs rather than continuing to test other plugins.")
    private boolean failFast = true;

    @Override
    public Integer call() throws PluginCompatibilityTesterException {
        try {
            Files.createDirectories(workingDir.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        PluginCompatTesterConfig config = new PluginCompatTesterConfig(war, workingDir);
        if (includePlugins != null) {
            config.setIncludePlugins(includePlugins);
        }
        if (excludePlugins != null) {
            config.setExcludePlugins(excludePlugins);
        }
        if (excludeHooks != null) {
            config.setExcludeHooks(excludeHooks);
        }
        config.setFallbackGitHubOrganization(fallbackGitHubOrganization);
        config.setExternalMaven(externalMaven);
        config.setMavenSettings(mavenSettings);
        if (mavenProperties != null) {
            config.setMavenProperties(mavenProperties);
        }
        if (mavenArgs != null) {
            config.setMavenArgs(mavenArgs);
        }
        if (externalHooksJars != null) {
            config.setExternalHooksJars(externalHooksJars);
        }
        config.setLocalCheckoutDir(localCheckoutDir);
        config.setFailFast(failFast);

        PluginCompatTester tester = new PluginCompatTester(config);
        tester.testPlugins();
        return 0;
    }
}
