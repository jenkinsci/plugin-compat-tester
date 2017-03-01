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

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * POJO used to configure PluginCompatTester execution
 * @author Frederic Camblor
 */
public class PluginCompatTesterConfig {

    public static final String DEFAULT_UPDATE_CENTER_URL = "http://updates.jenkins-ci.org/update-center.json";
    public static final String DEFAULT_PARENT_GROUP = "org.jenkins-ci.plugins";
    public static final String DEFAULT_PARENT_ARTIFACT = "plugin";
    public static final String DEFAULT_PARENT_GAV = DEFAULT_PARENT_GROUP + ":" + DEFAULT_PARENT_ARTIFACT;

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

    private File war = null;

    private File externalMaven = null;

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
    // Allows to define a minimal cache threshold for TestStatus
    // That is to say, every results lower than this threshold won't be put
    // into the cache
    private TestStatus cacheThresholStatus = TestStatus.COMPILATION_ERROR;

    // Allows to provide XSL report file near XML report file
    // Only if reportFile is not null
    private boolean provideXslReport = true;

    // Allows to generate HTML Report file
    // Only if reportFile is not null
    private boolean generateHtmlReport = true;

    private String mavenPropertiesFile;

    // GoogleAppEngine property allowing to provide a security token to be able to write data
    private String gaeSecurityToken;
    // GoogleAppEngin base url for plugin compat tester
    private String gaeBaseUrl;
 
    // Classpath prefixes of the extra hooks
    private List<String> hookPrefixes = new ArrayList<String>(Arrays.asList("org.jenkins"));

    public PluginCompatTesterConfig(File workDirectory, File reportFile, File m2SettingsFile){
        this(DEFAULT_UPDATE_CENTER_URL, DEFAULT_PARENT_GAV,
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
            if(gavChunks.length == 3 && !"".equals(gavChunks[2])){
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

    public TestStatus getCacheThresholStatus() {
        return cacheThresholStatus;
    }

    public void setCacheThresholStatus(TestStatus cacheThresholStatus) {
        this.cacheThresholStatus = cacheThresholStatus;
    }

    public String getGaeSecurityToken() {
        return gaeSecurityToken;
    }

    public void setGaeSecurityToken(String gaeSecurityToken) {
        this.gaeSecurityToken = gaeSecurityToken;
    }

    public String getGaeBaseUrl() {
        return gaeBaseUrl;
    }

    public void setGaeBaseUrl(String gaeBaseUrl) {
        this.gaeBaseUrl = gaeBaseUrl;
    }

    public File getWar() {
        return war;
    }

    public void setWar(File war) {
        this.war = war;
    }

    public File getExternalMaven() {
        return externalMaven;
    }

    public void setExternalMaven(File externalMaven) {
        this.externalMaven = externalMaven;
    }

    public List<String> getHookPrefixes() {
        return hookPrefixes;
    }

    public void setHookPrefixes(List<String> hookPrefixes) {
        // Want to also process the default
        this.hookPrefixes.addAll(hookPrefixes);
    }
}
