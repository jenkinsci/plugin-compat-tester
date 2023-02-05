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

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * POJO containing CLI arguments &amp; help.
 *
 * @author Frederic Camblor
 */
public class CliOptions {
    @Parameter(
            names = "-war",
            required = true,
            description =
                    "A WAR file to scan for plugins rather than looking in the update center.")
    private File war;

    @CheckForNull
    @Parameter(
            names = "-testJDKHome",
            description = "A path to JDK HOME to be used for running tests in plugins.")
    private File testJDKHome;

    @CheckForNull
    @Parameter(
            names = "-testJavaArgs",
            description = "Java test arguments to be used for test runs.")
    private String testJavaArgs;

    @Parameter(
            names = "-workDirectory",
            required = true,
            description = "Work directory where plugin sources will be checked out")
    private File workDirectory;

    @Parameter(
            names = "-includePlugins",
            description =
                    "Comma separated list of plugins' artifactId to test.\n"
                            + "If not set, every plugin will be tested.")
    private String includePlugins;

    @Parameter(
            names = "-excludePlugins",
            description =
                    "Comma separated list of plugins' artifactId to NOT test.\n"
                            + "If not set, see includePlugins behaviour.")
    private String excludePlugins;

    @Parameter(
            names = "-excludeHooks",
            description =
                    "Comma separated list of hooks to NOT execute.\n"
                            + "If not set, all hooks will be executed.")
    private String excludeHooks;

    @Parameter(
            names = "-fallbackGitHubOrganization",
            description =
                    "Include an alternative organization to use as a fallback to download the"
                            + " plugin.\n"
                            + "It is useful to use your own fork releases for an specific plugin if the"
                            + " version is not found in the official repository.\n"
                            + "If set, The PCT will try to use the fallback if a plugin tag is not"
                            + " found in the regular URL.")
    private String fallbackGitHubOrganization;

    @Parameter(
            names = "-m2SettingsFile",
            description = "Maven settings file used while executing maven")
    private File m2SettingsFile;

    @Parameter(names = "-mvn", description = "External Maven executable")
    @CheckForNull
    private File externalMaven;

    @Parameter(
            names = "-mavenProperties",
            description =
                    "Define extra properties to be passed to the build. Format:"
                            + " 'KEY1=VALUE1:KEY2=VALUE2'. These options will be used a la -D.\n"
                            + "If your property values contain ':' you must use the"
                            + " 'mavenPropertiesFile' option instead.")
    private String mavenProperties;

    @Parameter(
            names = "-mavenPropertiesFile",
            description =
                    "Allow loading some maven properties from a file using the standard"
                            + " java.util.Properties file format. These options will be used a la -D")
    private String mavenPropertiesFile;

    @Parameter(
            names = "-mavenOptions",
            description =
                    "Options to pass to Maven (like -Pxxx; not to be confused with Java options,"
                            + " nor Maven properties).")
    private List<String> mavenOptions;

    @Parameter(names = "-hookPrefixes", description = "Prefixes of the extra hooks' classes")
    private String hookPrefixes;

    @Parameter(
            names = "-externalHooksJars",
            description = "Comma-separated list of external hooks jar file locations",
            listConverter = FileListConverter.class,
            validateWith = FileValidator.class)
    private List<File> externalHooksJars;

    @Parameter(
            names = "-localCheckoutDir",
            description =
                    "Folder containing either a local (possibly modified) clone of a plugin"
                            + " repository or a set of local clone of different plugins")
    private String localCheckoutDir;

    @Parameter(names = "-help", description = "Print this help message", help = true)
    private boolean printHelp;

    @Parameter(
            names = "-failFast",
            description =
                    "If multiple plugins are specified, fail the overall run after the first plugin"
                            + " failure occurs rather than continuing to test other plugins.")
    private boolean failFast = true;

    public File getWar() {
        return war;
    }

    public File getWorkDirectory() {
        return workDirectory;
    }

    public String getIncludePlugins() {
        return includePlugins;
    }

    public File getM2SettingsFile() {
        return m2SettingsFile;
    }

    public File getExternalMaven() {
        return externalMaven;
    }

    public String getExcludePlugins() {
        return excludePlugins;
    }

    public String getExcludeHooks() {
        return excludeHooks;
    }

    public String getFallbackGitHubOrganization() {
        return fallbackGitHubOrganization;
    }

    @CheckForNull
    public String getMavenProperties() {
        return mavenProperties;
    }

    @CheckForNull
    public String getMavenPropertiesFile() {
        return mavenPropertiesFile;
    }

    @NonNull
    public List<String> getMavenOptions() {
        return mavenOptions != null
                ? Collections.unmodifiableList(mavenOptions)
                : Collections.emptyList();
    }

    public String getHookPrefixes() {
        return hookPrefixes;
    }

    public List<File> getExternalHooksJars() {
        return externalHooksJars != null ? Collections.unmodifiableList(externalHooksJars) : null;
    }

    public String getLocalCheckoutDir() {
        return localCheckoutDir;
    }

    public boolean isPrintHelp() {
        return printHelp;
    }

    @CheckForNull
    public File getTestJDKHome() {
        return testJDKHome;
    }

    @CheckForNull
    public String getTestJavaArgs() {
        return testJavaArgs;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public static class FileListConverter implements IStringConverter<List<File>> {
        @Override
        public List<File> convert(String files) {
            String[] paths = files.split(",");
            List<File> fileList = new ArrayList<>();
            for (String path : paths) {
                fileList.add(new File(path));
            }
            return fileList;
        }
    }

    public static class FileValidator implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            for (String path : value.split(",")) {
                File jar = new File(path);
                if (!jar.exists() || !jar.isFile()) {
                    throw new ParameterException(
                            path + " must exists and be a normal file (not a directory)");
                }
            }
        }
    }
}
