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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;

import org.jenkins.tools.test.model.PCTPlugin;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatReport;
import org.jenkins.tools.test.model.PluginCompatResult;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PluginInfos;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.TestStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.ImmutableList;

import hudson.util.VersionNumber;

/**
 * Main test class for plugin compatibility test frontend
 *
 * @author Frederic Camblor
 */
public class PluginCompatTesterTest {

    private static final String MAVEN_INSTALLATION_WINDOWS = "C:\\Jenkins\\tools\\hudson.tasks.Maven_MavenInstallation\\mvn\\bin\\mvn.cmd";

    private static final String REPORT_FILE = String.format("%s%sreports%sPluginCompatReport.xml",
            System.getProperty("java.io.tmpdir"), File.separator, File.separator);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        SCMManagerFactory.getInstance().start();
        File file = Paths.get(REPORT_FILE).toFile();
        if (file.exists()) {
            FileUtils.deleteQuietly(file);
        }
    }

    @After
    public void tearDown() {
        SCMManagerFactory.getInstance().stop();
    }

    @Ignore("TODO broken by https://github.com/jenkinsci/active-directory-plugin/releases/tag/active-directory-2.17; figure out how to pin a version")
    @Test
    public void testWithUrl() throws Throwable {
        PluginCompatTesterConfig config = getConfig(ImmutableList.of("active-directory"));
        config.setStoreAll(true); 

        PluginCompatTester tester = new PluginCompatTester(config);
        PluginCompatReport report = tester.testPlugins();
        assertNotNull(report);
        Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests = report.getPluginCompatTests();
        assertNotNull(pluginCompatTests);
        for (Entry<PluginInfos, List<PluginCompatResult>> entry : pluginCompatTests.entrySet()) {
            assertEquals("active-directory", entry.getKey().pluginName);
            List<PluginCompatResult> results = entry.getValue();
            assertEquals(1, results.size());
            PluginCompatResult result = results.get(0);
            assertNotNull(result);
            assertNotNull(result.getTestsDetails());
            assertFalse(result.getTestsDetails().isEmpty());
            // Let's evaluate some executed tests 
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectoryAuthenticationProviderTest.testEscape"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testAdvancedOptionsVisibleWithNonNativeAuthentication"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testCacheOptionAlwaysVisible"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveMultiDomainSingleDomainOneDisplayName"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveMultiDomainTwoDomainsOneDisplayName"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveMultipleDomainsOneDomainEndToEnd"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveSingleDomain"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveSingleDomainSingleServer"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveSingleDomainWithTwoServers"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveTwoDomainsWithSpaceAfterComma"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveTwoDomainsWithSpaceAfterCommaAndSingleServer"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveTwoDomainsWithSpaceAfterCommaAndTwoServers"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveTwoDomainsWithoutSpaceAfterComma"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveTwoDomainsWithoutSpaceAfterCommaAndSingleServer"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectorySecurityRealmTest.testReadResolveTwoDomainsWithoutSpaceAfterCommaAndTwoServers"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.RemoveIrrelevantGroupsTest.testNoGroupsAreRegistered"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.RemoveIrrelevantGroupsTest.testNoGroupsAreRelevant"));
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.RemoveIrrelevantGroupsTest.testSomeGroupsAreRelevant"));
        }
    }
    
    @Ignore("TODO broken by https://github.com/jenkinsci/active-directory-plugin/releases/tag/active-directory-2.17; figure out how to pin a version")
    @Test
    public void testWithIsolatedTest() throws Throwable {
        PluginCompatTesterConfig config = getConfig(ImmutableList.of("active-directory"));
        config.setStoreAll(true); 
        Map<String, String> mavenProperties = new HashMap<>();
        mavenProperties.put("test","ActiveDirectoryAuthenticationProviderTest#testEscape");
        config.setMavenProperties(mavenProperties);

        PluginCompatTester tester = new PluginCompatTester(config);
        PluginCompatReport report = tester.testPlugins();
        assertNotNull(report);
        Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests = report.getPluginCompatTests();
        assertNotNull(pluginCompatTests);
        for (Entry<PluginInfos, List<PluginCompatResult>> entry : pluginCompatTests.entrySet()) {
            assertEquals("active-directory", entry.getKey().pluginName);
            List<PluginCompatResult> results = entry.getValue();
            assertEquals(1, results.size());
            PluginCompatResult result = results.get(0);
            assertNotNull(result);
            assertNotNull(result.getTestsDetails());
            assertFalse(result.getTestsDetails().isEmpty());
            assertEquals(1, result.getTestsDetails().size());
            assertTrue(result.getTestsDetails().contains("hudson.plugins.active_directory.ActiveDirectoryAuthenticationProviderTest.testEscape"));
        }
    }  
    
    @Test
    public void testStoreOnlyFailedTests() throws Throwable {
        PluginCompatTesterConfig config = getConfig(ImmutableList.of("analysis-collector"));
        config.setStoreAll(false);

        PluginCompatTester tester = new PluginCompatTester(config);
        PluginCompatReport report = tester.testPlugins();
        assertNotNull(report);
        Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests = report.getPluginCompatTests();
        assertNotNull(pluginCompatTests);
        for (Entry<PluginInfos, List<PluginCompatResult>> entry : pluginCompatTests.entrySet()) {
            assertEquals("analysis-collector", entry.getKey().pluginName);
            List<PluginCompatResult> results = entry.getValue();
            assertEquals(1, results.size());
            PluginCompatResult result = results.get(0);
            assertNotNull(result);
            assertNotNull(result.getTestsDetails());
            // No failed tests on this plugin (it will store ONLY failed tests due to -storeAll=false
            assertTrue(result.getTestsDetails().isEmpty());
        }
    } 

    @Test
    public void testBom() throws IOException, PlexusContainerException, PomExecutionException, XmlPullParserException {
        PluginCompatTesterConfig config = getConfig(ImmutableList.of("workflow-api", // From BOM
                "accurev" // From Update Center
        ));

        File bomFile = new ClassPathResource("jenkins-bom.xml").getFile();
        config.setBom(bomFile);

        PluginCompatTester tester = new PluginCompatTester(config);
        tester.testPlugins();
    }  
    
    @Test
    public void testWithCasCProperties() throws Throwable {
        PluginCompatTesterConfig config = getConfig(ImmutableList.of("ec2"));
        Map<String, String> mavenProperties = new HashMap<>();
        mavenProperties.put("test","ConfigurationAsCodeTest#testConfigAsCodeExport");
        config.setMavenProperties(mavenProperties);
        List<PCTPlugin> overridenPlugins = Arrays.asList(new PCTPlugin("configuration-as-code", "io.jenkins", new VersionNumber("1.38")));
        config.setOverridenPlugins(overridenPlugins);

        PluginCompatTester tester = new PluginCompatTester(config);
        tester.testPlugins();
    }
    
    @Test
    public void testWithInvalidExclusionList() throws Throwable {
        File exclusionList = new ClassPathResource("bad-surefire-exclusion-list").getFile();
        PluginCompatTesterConfig config = getConfig(ImmutableList.of("active-directory"));
        Map<String, String> mavenProperties = new HashMap<>();
        mavenProperties.put("surefire.excludesFile", exclusionList.getAbsolutePath());
        config.setMavenProperties(mavenProperties);
        
        PluginCompatTester tester = new PluginCompatTester(config);
        PluginCompatReport report = tester.testPlugins();
        assertNotNull(report);
        Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests = report.getPluginCompatTests();
        assertNotNull(pluginCompatTests);
        for (Entry<PluginInfos, List<PluginCompatResult>> entry : pluginCompatTests.entrySet()) {
            assertEquals("active-directory", entry.getKey().pluginName);
            List<PluginCompatResult> results = entry.getValue();
            assertEquals(1, results.size());
            PluginCompatResult result = results.get(0);
            assertNotNull(result);
            assertNotNull(result.status);
            assertEquals(TestStatus.INTERNAL_ERROR, result.status);
        }
    }

    private PluginCompatTesterConfig getConfig(List<String> includedPlugins) throws IOException {
        PluginCompatTesterConfig config = new PluginCompatTesterConfig(testFolder.getRoot(),
                new File(REPORT_FILE), getSettingsFile());      

        config.setIncludePlugins(includedPlugins);
        config.setExcludePlugins(Collections.emptyList());
        config.setSkipTestCache(true);
        config.setCacheThresholdStatus(TestStatus.TEST_FAILURES);
        config.setTestCacheTimeout(345600000);
        config.setParentVersion("1.410");
        config.setGenerateHtmlReport(true);
        config.setHookPrefixes(Collections.emptyList());

        File ciJenkinsIoWinMvn = Paths.get(MAVEN_INSTALLATION_WINDOWS).toFile();
        if (ciJenkinsIoWinMvn.exists()) {
            System.out.println(String.format("Using mvn: %s", MAVEN_INSTALLATION_WINDOWS));
            config.setExternalMaven(ciJenkinsIoWinMvn);
        }

        return config;
    }

    @Ignore("TODO broken by GH protocol changes. Requesting user data access via https on local execution")
    @Test(expected = RuntimeException.class)
    public void testWithoutAlternativeUrl() throws Throwable {
        String pluginName = "workflow-api";
        String version = "2.39";
        String nonWorkingConnectionURL = "scm:git:git://github.com/test/workflow-api-plugin.git";
        MavenCoordinates mavenCoordinates = new MavenCoordinates("org.jenkins-ci.plugins", "plugin", "3.54");
        PluginCompatTesterConfig config = new PluginCompatTesterConfig(testFolder.getRoot(), new File(REPORT_FILE),
                getSettingsFile());
        config.setIncludePlugins(ImmutableList.of(pluginName));

        PluginCompatTester pct = new PluginCompatTester(config);
        PomData pomData = new PomData(pluginName, "hpi", nonWorkingConnectionURL, pluginName + "-" + version,
                mavenCoordinates, "org.jenkins-ci.plugins.workflow");
        pct.cloneFromSCM(pomData, pluginName, version,
                new File(config.workDirectory.getAbsolutePath() + File.separator + pluginName + File.separator), "");
    }

    @Ignore("TODO broken by GH protocol changes. Requesting user data access via https on local execution")
    @Test(expected = Test.None.class)
    public void testWithAlternativeUrl() throws Throwable {
        String pluginName = "workflow-api";
        String version = "2.39";
        String nonWorkingConnectionURL = "scm:git:git://github.com/test/workflow-api-plugin.git";
        MavenCoordinates mavenCoordinates = new MavenCoordinates("org.jenkins-ci.plugins", "plugin", "3.54");
        PluginCompatTesterConfig config = new PluginCompatTesterConfig(testFolder.getRoot(), new File(REPORT_FILE),
                getSettingsFile());
        config.setIncludePlugins(ImmutableList.of(pluginName));
        config.setFallbackGitHubOrganization("jenkinsci");

        PluginCompatTester pct = new PluginCompatTester(config);
        PomData pomData = new PomData(pluginName, "hpi", nonWorkingConnectionURL, pluginName + "-" + version,
                mavenCoordinates, "org.jenkins-ci.plugins.workflow");
        pct.cloneFromSCM(pomData, pluginName, version,
                new File(config.workDirectory.getAbsolutePath() + File.separator + pluginName + File.separator), "");
    }

    @Test
    public void testMatcher() {

        String fileName = "WEB-INF/lib/jenkins-core-2.7.3-alpha-33.jar";
        Matcher m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-alpha-33", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-ALPHA-33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-ALPHA-33", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-ALPHA-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-ALPHA-33-SNAPSHOT", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-alpha-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-alpha-33-SNAPSHOT", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-beta-33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-beta-33", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-BETA-33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-BETA-33", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-BETA-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-BETA-33-SNAPSHOT", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-BETA-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-BETA-33-SNAPSHOT", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-rc-33", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-RC-33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-RC-33", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-RC-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-RC-33-SNAPSHOT", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-rc-33-SNAPSHOT", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-SNAPSHOT", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-RC33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-RC33", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-alpha33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-alpha33-SNAPSHOT", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-rc-SNAPSHOT", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-milestone.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-milestone", m.group(1));

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-milestone.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
        assertEquals("Invalid group", "2.7.3-rc-milestone", m.group(1));
    }

    @Test
    @Issue("JENKINS-50454")
    public void testCustomWarPackagerVersions() {
        // TODO: needs more filtering
        String fileName = "WEB-INF/lib/jenkins-core-256.0-my-branch-2090468d82e49345519a2457f1d1e7426f01540b-SNAPSHOT.jar";
        Matcher m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());

        fileName = "WEB-INF/lib/jenkins-core-256.0-2090468d82e49345519a2457f1d1e7426f01540b-2090468d82e49345519a2457f1d1e7426f01540b-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue("No matches", m.matches());
    }

    private static File getSettingsFile() throws IOException {
        // Check whether we run in ci.jenkins.io with Azure settings
        File ciJenkinsIOSettings = new File(new File("settings-azure.xml").getAbsolutePath()
                .replace("/plugins-compat-tester/settings-azure.xml", "@tmp/settings-azure.xml"));
        System.out.println("Will check Maven settings from " + ciJenkinsIOSettings.getAbsolutePath());
        if (ciJenkinsIOSettings.exists()) {
            System.out.println("Will use the ci.jenkins.io Azure settings file for testing: "
                    + ciJenkinsIOSettings.getAbsolutePath());
            return ciJenkinsIOSettings;
        }
        // Default fallback for local runs
        return new ClassPathResource("m2-settings.xml").getFile();
    }

}