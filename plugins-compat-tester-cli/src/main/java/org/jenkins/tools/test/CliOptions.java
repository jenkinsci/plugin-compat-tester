package org.jenkins.tools.test;

import com.beust.jcommander.Parameter;

import java.io.File;
import java.util.List;

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

    @Parameter(names = "-m2SettingsFile",
            description = "Maven settings file used while executing maven")
    private File m2SettingsFile;

    @Parameter(names = "-skipTestCache",
            description = "Allows to skip compat test cache (by default, to 1003 days)\n" +
                    "If set to true, every plugin will be tested, no matter the cache is.")
    private String skipTestCache = null;

    @Parameter(names = "-testCacheTimeout",
            description = "Allows to override the test cache timeout.\n" +
                    "Test cache timeout allows to not perform compatibility test over\n" +
                    "some plugins if compatibility test was performed recently.\n" +
                    "Cache timeout is given in milliseconds")
    private Long testCacheTimeout = null;


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

    public String getSkipTestCache() {
        return skipTestCache;
    }

    public Long getTestCacheTimeout() {
        return testCacheTimeout;
    }
}
