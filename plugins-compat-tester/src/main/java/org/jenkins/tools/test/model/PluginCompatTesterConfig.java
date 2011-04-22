package org.jenkins.tools.test.model;

import java.io.File;
import java.util.List;

public class PluginCompatTesterConfig {

    public final String updateCenterUrl;
    public final String parentGroupId;
    public final String parentArtifactId;
    public final File workDirectory;
    public final File reportFile;
    private final File m2SettingsFile;

    private String parentVersion = null;
    private List<String> pluginsList = null;

    public PluginCompatTesterConfig(File workDirectory, File reportFile, File m2SettingsFile){
        this("http://updates.jenkins-ci.org/update-center.json?version=build", "org.jenkins-ci.plugins:plugin",
                m2SettingsFile, workDirectory, reportFile);
    }

    public PluginCompatTesterConfig(String updateCenterUrl, String parentGAV, File m2SettingsFile,
                                    File workDirectory, File reportFile){
        this.updateCenterUrl = updateCenterUrl;
        String[] gavChunks = parentGAV.split(":");
        assert gavChunks.length == 3 || gavChunks.length == 2;
        this.parentGroupId = gavChunks[0];
        this.parentArtifactId = gavChunks[1];
        if(gavChunks.length == 3){
            this.setParentVersion(gavChunks[2]);
        }
        this.workDirectory = workDirectory;
        this.reportFile = reportFile;
        this.m2SettingsFile = m2SettingsFile;
    }

    public String getParentVersion() {
        return parentVersion;
    }

    public void setParentVersion(String parentVersion) {
        this.parentVersion = parentVersion;
    }

    public List<String> getPluginsList() {
        return pluginsList;
    }

    public void setPluginsList(List<String> pluginsList) {
        this.pluginsList = pluginsList;
    }

    public File getM2SettingsFile() {
        return m2SettingsFile;
    }
}
