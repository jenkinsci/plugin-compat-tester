package org.jenkins.tools.test;

import com.beust.jcommander.Parameter;

import java.io.File;
import java.util.List;

public class CliOptions {
    @Parameter(names = "-updateCenterUrl", description = "Update center JSON file URL")
    private String updateCenterUrl = "http://updates.jenkins-ci.org/update-center.json?version=build";

    @Parameter(names = "-parentCoordinates", description = "Parent pom GAV in the form groupId:artifactId[:version].")
    private String parentCoord = "org.jenkins-ci.plugins:plugin";

    @Parameter(names = "-workDirectory", required = true, description = "Work directory where plugin sources will be checkouted")
    private File workDirectory;

    @Parameter(names = "-reportFile", required = true, description = "Output report xml file path")
    private File reportFile;

    @Parameter(names = "-pluginsList", description = "Comma separated list of plugins' artifactId to test\nIf not set, every plugin will be tested.")
    private String pluginsList = null;

    @Parameter(names = "-m2SettingsFile", description = "Maven settings file used while executing maven")
    private File m2SettingsFile;

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

    public String getPluginsList() {
        return pluginsList;
    }

    public File getM2SettingsFile() {
        return m2SettingsFile;
    }
}
