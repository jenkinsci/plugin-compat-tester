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
import hudson.util.VersionNumber;
import java.io.File;
import java.util.List;
import javax.annotation.CheckForNull;
import org.jenkins.tools.test.model.PCTPlugin;
import org.jenkins.tools.test.model.TestStatus;

/**
 * POJO containing CLI arguments &amp; help.
 *
 * @author Frederic Camblor
 */
public class CliOptions {
    @Parameter(names = "-updateCenterUrl",
            description = "Update center JSON file URL")
    private String updateCenterUrl = null;

    @Parameter(names = "-parentCoordinates",
            description = "Parent pom GAV in the form groupId:artifactId[:version].\n" +
                    "If null/empty, every core coordinates located in report XML files will be tested.")
    private String parentCoord = null;

    @Parameter(names = "-war",
            description = "A WAR file to scan for plugins rather than looking in the update center.")
    private File war = null;

    @CheckForNull
    @Parameter(names = "-testJDKHome",
            description = "A path to JDK HOME to be used for running tests in plugins.")
    private File testJDKHome = null;

    @CheckForNull
    @Parameter(names = "-testJavaArgs",
            description = "Java test arguments to be used for test runs.")
    private String testJavaArgs = null;

    @Parameter(names = "-workDirectory", required = true,
            description = "Work directory where plugin sources will be checked out")
    private File workDirectory;

    @Parameter(names = "-reportFile", required = true,
            description = "Output report xml file path. This path must contain a directory, e.g. 'out/pct-report.xml'")
    private File reportFile;

    @Parameter(names = "-includePlugins",
            description = "Comma separated list of plugins' artifactId to test.\n" +
                    "If not set, every plugin will be tested.")
    private String includePlugins = null;

    @Parameter(names = "-excludePlugins",
            description = "Comma separated list of plugins' artifactId to NOT test.\n" +
                    "If not set, see includePlugins behaviour.")
    private String excludePlugins = null;

    @Parameter(names = "-fallbackGitHubOrganization",
            description = "Include an alternative organization to use as a fallback to download the plugin.\n" +
                    "It is useful to use your own fork releases for an specific plugin if the " +
                    "version is not found in the official repository.\n" +
                    "If set, The PCT will try to use the fallback if a plugin tag is not found in the regular URL.")
    private String fallbackGitHubOrganization = null;

    @Parameter(names = "-m2SettingsFile",
            description = "Maven settings file used while executing maven")
    private File m2SettingsFile;

    @Parameter(names = "-mvn",
            description = "External Maven executable")
    @CheckForNull
    private File externalMaven;

    @Parameter(names = "-skipTestCache",
            description = "Allows to skip compatibility test cache (by default, to 100 days)\n" +
                    "If set to true, every plugin will be tested, no matter the cache is.")
    private String skipTestCache = null;

    @Parameter(names = "-testCacheTimeout",
            description = "Allows to override the test cache timeout.\n" +
                    "Test cache timeout allows to not perform compatibility test over\n" +
                    "some plugins if compatibility test was performed recently.\n" +
                    "Cache timeout is given in milliseconds")
    private Long testCacheTimeout = null;

    @Parameter(names = "-cacheThresholdStatus",
            description = "Allows to define a minimal cache threshold for test status.\n" +
                    "That is to say, every results lower than this threshold won't be considered\n" +
                    "as part of the cache")
    private String cacheThresholdStatus = TestStatus.COMPILATION_ERROR.toString();

    @Parameter(names="-mavenProperties", description = "Define extra properties to be passed to the build." +
          "Format: 'KEY1=VALUE1:KEY2=VALUE2'. These options will be used a la -D.\n" +
            "If your property values contain ':' you must use the 'mavenPropertiesFile' option instead.")
    private String mavenProperties;

    @Parameter(names="-mavenPropertiesFile", description = "Allow loading some maven properties from a file using the standard java.util.Properties file format. " +
            "These options will be used a la -D")
    private String mavenPropertiesFile;

    @Parameter(names="-hookPrefixes", description = "Prefixes of the extra hooks' classes")
    private String hookPrefixes;

    @Parameter(names="-localCheckoutDir", description = "Folder containing either a local (possibly modified) clone of a plugin repository or a set of local clone of different plugins")
    private String localCheckoutDir;

    @Parameter(names="-help", description = "Print this help message", help = true)
    private boolean printHelp;

    @Parameter(names = "-overridenPlugins", description = "List of plugins to use to test a plugin in place of the normal dependencies." +
          "Format: '[GROUP_ID:]PLUGIN_NAME=PLUGIN_VERSION", converter = PluginConverter.class, validateWith = PluginValidator.class)
    private List<PCTPlugin> overridenPlugins;

    @Parameter(names = "-failOnError", description = "Immediately if the PCT run fails for a plugin. Error status will be also reported as a return code")
    private boolean failOnError;
    
    @Parameter(names = "-storeAll", 
            description = "By default only failed tests are stored in PCT report file. \n" + 
                    "If set, the PCT will store ALL executed test names for each plugin in report file. \n" + 
                    "Disabled by default because it may bloat reports for plugins which thousands of tests."
    )
    private Boolean storeAll = null;

    @Parameter(names = "-bom", description = "BOM file to be used for plugin versions rather than an Update Center or War file")
    private File bom;

    public String getUpdateCenterUrl() {
        return updateCenterUrl;
    }

    public String getParentCoord() {
        return parentCoord;
    }

    public File getWar() {
        return war;
    }

    public File getReportFile() {
        return reportFile;
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

    public String getSkipTestCache() {
        return skipTestCache;
    }

    public Long getTestCacheTimeout() {
        return testCacheTimeout;
    }

    public String getExcludePlugins() {
        return excludePlugins;
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

    public String getCacheThresholdStatus() {
        return cacheThresholdStatus;
    }

    public String getHookPrefixes() {
        return hookPrefixes;
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

    @CheckForNull
    public List<PCTPlugin> getOverridenPlugins() {
        return overridenPlugins;
    }

    @CheckForNull
    public File getBOM() {
        return this.bom;
    }

    public boolean isFailOnError() {
        return failOnError;
    }
    
    public Boolean isStoreAll() {
        return storeAll;
    }

    public static class PluginConverter implements IStringConverter<PCTPlugin> {
        @Override
        public PCTPlugin convert(String s) {
            String[] details = s.split("=");
            String name = details[0];
            int colon = name.indexOf(':');
            return new PCTPlugin(colon == -1 ? name : name.substring(colon + 1), colon == -1 ? null : name.substring(0, colon), new VersionNumber(details[1]));
        }
    }

    public static class PluginValidator implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            for (String s : value.split(",")) {
                final String[] split = s.split("=");
                if (split.length != 2) {
                    throw new ParameterException(name + " must be formatted as NAME=VERSION");
                }
            }
        }
    }
}
