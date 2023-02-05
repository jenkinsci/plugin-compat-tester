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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.logging.LoggingConfiguration;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

/**
 * Plugin compatibility tester frontend for the CLI
 *
 * @author Frederic Camblor
 */
public class PluginCompatTesterCli {

    static {
        String configFile = System.getProperty("java.util.logging.config.file");
        String configClass = System.getProperty("java.util.logging.config.class");
        if (configClass == null && configFile == null) {
            new LoggingConfiguration();
        }
    }

    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification =
                    "We're already checking for null in each relevant instance, so why does"
                            + " SpotBugs complain?")
    public static void main(String[] args) throws PluginCompatibilityTesterException {
        CliOptions options = new CliOptions();
        JCommander jcommander = JCommander.newBuilder().addObject(options).build();
        try {
            jcommander.parse(args);
            if (options.isPrintHelp()) {
                jcommander.usage();
                return;
            }
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jcommander.usage();
            System.exit(1);
        }

        try {
            Files.createDirectories(options.getWorkDirectory().toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        PluginCompatTesterConfig config = new PluginCompatTesterConfig();

        config.setWorkDirectory(options.getWorkDirectory());
        config.setM2SettingsFile(options.getM2SettingsFile());
        config.setWar(options.getWar());
        config.setExternalMaven(options.getExternalMaven());

        if (options.getIncludePlugins() != null && !options.getIncludePlugins().isEmpty()) {
            config.setIncludePlugins(List.of(options.getIncludePlugins().toLowerCase().split(",")));
        }
        if (options.getExcludePlugins() != null && !options.getExcludePlugins().isEmpty()) {
            config.setExcludePlugins(List.of(options.getExcludePlugins().toLowerCase().split(",")));
        }
        if (options.getExcludeHooks() != null && !options.getExcludeHooks().isEmpty()) {
            config.setExcludeHooks(List.of(options.getExcludeHooks().split(",")));
        }
        if (options.getHookPrefixes() != null && !options.getHookPrefixes().isEmpty()) {
            config.setHookPrefixes(List.of(options.getHookPrefixes().split(",")));
        }
        if (options.getExternalHooksJars() != null && !options.getExternalHooksJars().isEmpty()) {
            config.setExternalHooksJars(options.getExternalHooksJars());
        }
        if (options.getLocalCheckoutDir() != null && !options.getLocalCheckoutDir().isEmpty()) {
            config.setLocalCheckoutDir(options.getLocalCheckoutDir());
        }
        if (options.getTestJDKHome() != null) {
            config.setTestJDKHome(options.getTestJDKHome());
        }
        if (options.getTestJavaArgs() != null && !options.getTestJavaArgs().isEmpty()) {
            config.setTestJavaArgs(options.getTestJavaArgs());
        }
        if (options.getFallbackGitHubOrganization() != null) {
            config.setFallbackGitHubOrganization(options.getFallbackGitHubOrganization());
        }
        if (options.isFailFast()) {
            config.setFailFast(true);
        }

        // Handle properties
        if (options.getMavenProperties() != null) {
            String[] split = options.getMavenProperties().split("\\s*:\\s*");
            Map<String, String> mavenProps = new HashMap<>(split.length);
            for (String expr : split) {
                String[] split2 = expr.split("=", 2);
                String key = split2[0];
                String value = split2.length == 2 ? split2[1] : null;
                mavenProps.put(key, value);
            }
            config.setMavenProperties(mavenProps);
        }
        // TODO: Read property file here as well? No sense to do it letter unless PCT Hooks modify
        // file
        if (options.getMavenPropertiesFile() != null) {
            config.setMavenPropertiesFiles(options.getMavenPropertiesFile());
        }

        config.setMavenOptions(options.getMavenOptions());

        PluginCompatTester tester = new PluginCompatTester(config);
        tester.testPlugins();
    }
}
