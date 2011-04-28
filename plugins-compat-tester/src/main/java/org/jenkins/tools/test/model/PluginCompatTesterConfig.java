package org.jenkins.tools.test.model;

import java.io.File;
import java.util.List;

public class PluginCompatTesterConfig {

    // Update center used to retrieve plugins informations
    public final String updateCenterUrl;

    // A working directory where will be checkouted tested plugin's sources
    public final File workDirectory;

    // A report file where will be generated testing report
    // If the file already exist, testing report will be merged into it
    public final File reportFile;

    // Path for maven settings file where repository will be provided allowing to
    // download jenkins-core artefact (and dependencies)
    private final File m2SettingsFile;

    // GroupId which will be used to replace tested plugin's parent groupId
    // If null, every recorded core coordinates (in report xml) will be played
    private String parentGroupId = null;
    // ArtifactId which will be used to replace tested plugin's parent artifactId
    // If null, every recorded core coordinates (in report xml) will be played
    private String parentArtifactId = null;
    // Version which will be used to replace tested plugin's parent verison
    // If null, latest core version (retrieved via the update center) will be used
    private String parentVersion = null;

    // List of plugin artefact ids on which tests will be performed
    // If null, tests will be performed on every plugins retrieved from update center
    private List<String> includePlugins = null;

    // List of plugin artefact ids on which tests will be not performed
    // If null, tests will be performed on every includePlugins found
    private List<String> excludePlugins = null;

    // Allows to skip a plugin test if this plugin test has already been performed
    // within testCacheTimeout ms
    private long testCacheTimeout = 1000*60*60*24*100;
    // Skips test cache : plugin will be tested, no matter the test cache is
    private boolean skipTestCache = false;


    // Allows to provide XSL report file near XML report file
    // Only if reportFile is not null
    private boolean provideXslReport = true;

    // Allows to generate HTML Report file
    // Only if reportFile is not null
    private boolean generateHtmlReport = true;


    private String mavenPropertiesFile;

    public PluginCompatTesterConfig(File workDirectory, File reportFile, File m2SettingsFile){
        this("http://updates.jenkins-ci.org/update-center.json?version=build", "org.jenkins-ci.plugins:plugin",
                workDirectory, reportFile, m2SettingsFile);
    }

    public PluginCompatTesterConfig(String updateCenterUrl, String parentGAV,
                                    File workDirectory, File reportFile, File m2SettingsFile){
        this.updateCenterUrl = updateCenterUrl;
        if(parentGAV != null && !"".equals(parentGAV)){
            String[] gavChunks = parentGAV.split(":");
            assert gavChunks.length == 3 || gavChunks.length == 2;
            this.parentGroupId = gavChunks[0];
            this.parentArtifactId = gavChunks[1];
            if(gavChunks.length == 3){
                this.setParentVersion(gavChunks[2]);
            }
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

    public List<String> getIncludePlugins() {
        return includePlugins;
    }

    public void setIncludePlugins(List<String> pluginsList) {
        this.includePlugins = pluginsList;
    }

    public File getM2SettingsFile() {
        return m2SettingsFile;
    }

    public long getTestCacheTimeout() {
        return testCacheTimeout;
    }

    public void setTestCacheTimeout(long testCacheTimeout) {
        this.testCacheTimeout = testCacheTimeout;
    }

    public boolean isSkipTestCache() {
        return skipTestCache;
    }

    public void setSkipTestCache(boolean skipTestCache) {
        this.skipTestCache = skipTestCache;
    }

    public boolean isProvideXslReport() {
        return provideXslReport;
    }

    public void setProvideXslReport(boolean provideXslReport) {
        this.provideXslReport = provideXslReport;
    }

    public boolean isGenerateHtmlReport() {
        return generateHtmlReport;
    }

    public void setGenerateHtmlReport(boolean generateHtmlReport) {
        this.generateHtmlReport = generateHtmlReport;
    }

    public String getParentGroupId() {
        return parentGroupId;
    }

    public String getParentArtifactId() {
        return parentArtifactId;
    }

    public List<String> getExcludePlugins() {
        return excludePlugins;
    }

    public void setExcludePlugins(List<String> excludePlugins) {
        this.excludePlugins = excludePlugins;
    }

    public String getMavenPropertiesFile() {
        return mavenPropertiesFile;
    }

    public void setMavenPropertiesFiles( String mavenPropertiesFile ) {
        this.mavenPropertiesFile = mavenPropertiesFile;
    }
}
