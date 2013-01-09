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

import com.beust.jcommander.Parameter;
import org.jenkins.tools.test.model.TestStatus;

import java.io.File;
import java.util.List;

/**
 * POJO containing CLI arguments & help
 * @author Frederic Camblor
 */
public class CliOptions {
    @Parameter(names = "-updateCenterUrl",
            description = "Update center JSON file URL")
    private String updateCenterUrl = "http://updates.jenkins-ci.org/update-center.json?version=build";

    @Parameter(names = "-parentCoordinates",
            description = "Parent pom GAV in the form groupId:artifactId[:version].\n" +
                    "If null/empty, every core coordinates located in report XML files will be tested.")
    private String parentCoord = "org.jenkins-ci.plugins:plugin";

    @Parameter(names = "-workDirectory", required = true,
            description = "Work directory where plugin sources will be checkouted")
    private File workDirectory;

    @Parameter(names = "-reportFile", required = true,
            description = "Output report xml file path")
    private File reportFile;

    @Parameter(names = "-includePlugins",
            description = "Comma separated list of plugins' artifactId to test.\n" +
                    "If not set, every plugin will be tested.")
    private String includePlugins = null;

    @Parameter(names = "-excludePlugins",
            description = "Comma separated list of plugins' artifactId to NOT test.\n" +
                    "If not set, see includePlugins behaviour.")
    private String excludePlugins = null;

    @Parameter(names = "-m2SettingsFile",
            description = "Maven settings file used while executing maven")
    private File m2SettingsFile;

    @Parameter(names = "-skipTestCache",
            description = "Allows to skip compat test cache (by default, to 100 days)\n" +
                    "If set to true, every plugin will be tested, no matter the cache is.")
    private Boolean skipTestCache = null;

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

    @Parameter(names="-mavenProperties", description = "allow to load some maven properties which will be used a la -D")
    private String mavenPropertiesFile;

    @Parameter(names="-gaeSecurityToken", description = "Allows to pass GAE Security token needed to write data")
    private String gaeSecurityToken;
    @Parameter(names="-gaeBaseUrl", description = "Allows to pass GAE plugin compat tester base url")
    private String gaeBaseUrl;


    public String getUpdateCenterUrl() {
        return updateCenterUrl;
    }

    public String getParentCoord() {
        return parentCoord;
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

    public Boolean getSkipTestCache() {
        return skipTestCache;
    }

    public Long getTestCacheTimeout() {
        return testCacheTimeout;
    }

    public String getExcludePlugins() {
        return excludePlugins;
    }

    public String getMavenPropertiesFile() {
        return mavenPropertiesFile;
    }

    public String getCacheThresholdStatus() {
        return cacheThresholdStatus;
    }

    public String getGaeSecurityToken() {
        return gaeSecurityToken;
    }

    public String getGaeBaseUrl() {
        return gaeBaseUrl;
    }
}
