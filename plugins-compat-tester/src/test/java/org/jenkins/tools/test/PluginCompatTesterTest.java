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

import com.google.common.collect.ImmutableList;
import java.io.File;

import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatReport;
import org.jenkins.tools.test.model.PluginCompatResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PluginInfos;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.TestStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.springframework.core.io.ClassPathResource;

/**
 * Main test class for plugin compatibility test frontend
 *
 * @author Frederic Camblor
 */
public class PluginCompatTesterTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        SCMManagerFactory.getInstance().start();
    }

    @After
    public void tearDown() {
        SCMManagerFactory.getInstance().stop();
    }

    @Test
    public void testWithUrl() throws Throwable {
        ImmutableList<String> includedPlugins = ImmutableList.of("accurev"
        /*
         * "active-directory", "analysis-collector", "scm-sync-configuration"
         */
        );

        PluginCompatTesterConfig config = new PluginCompatTesterConfig(testFolder.getRoot(),
                new File("../reports/PluginCompatReport.xml"), getSettingsFile());

        config.setIncludePlugins(includedPlugins);
        config.setSkipTestCache(true);
        config.setCacheThresholdStatus(TestStatus.TEST_FAILURES);
        config.setTestCacheTimeout(345600000);
        config.setParentVersion("1.410");
        config.setGenerateHtmlReport(true);

        PluginCompatTester tester = new PluginCompatTester(config);
        PluginCompatReport report = tester.testPlugins();
        assertNotNull(report);
        Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests = report.getPluginCompatTests();
        assertNotNull(pluginCompatTests);
        for (Entry<PluginInfos, List<PluginCompatResult>> entry : pluginCompatTests.entrySet()) {
            assertEquals("accurev", entry.getKey().pluginName);
            List<PluginCompatResult> results = entry.getValue();
            assertEquals(1, results.size());
            PluginCompatResult result = results.get(0);
            assertNotNull(result);
            assertNotNull(result.getExecutedTests());
        }
    }
    
    @Test
    public void testWithIsolatedTest() throws Throwable {
        ImmutableList<String> includedPlugins = ImmutableList.of("accurev");

        PluginCompatTesterConfig config = new PluginCompatTesterConfig(testFolder.getRoot(),
                new File("../reports/PluginCompatReport.xml"), getSettingsFile());

        config.setIncludePlugins(includedPlugins);
        config.setSkipTestCache(true);
        config.setCacheThresholdStatus(TestStatus.TEST_FAILURES);
        config.setTestCacheTimeout(345600000);
        config.setParentVersion("1.410");
        config.setGenerateHtmlReport(true);
        Map<String, String> mavenProperties = new HashMap<>();
        mavenProperties.put("test","AccurevSCMTest#testConfigRoundtrip");
        config.setMavenProperties(mavenProperties);

        PluginCompatTester tester = new PluginCompatTester(config);
        PluginCompatReport report = tester.testPlugins();
        assertNotNull(report);
        Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests = report.getPluginCompatTests();
        assertNotNull(pluginCompatTests);
        for (Entry<PluginInfos, List<PluginCompatResult>> entry : pluginCompatTests.entrySet()) {
            assertEquals("ant", entry.getKey().pluginName);
            List<PluginCompatResult> results = entry.getValue();
            assertEquals(1, results.size());
            PluginCompatResult result = results.get(0);
            assertNotNull(result);
            assertNotNull(result.getExecutedTests());
            assertEquals(1, result.getExecutedTests().size());
            assertTrue(result.getExecutedTests().contains("hudson.plugins.accurev.AccurevSCMTest.testConfigRoundtrip"));
        }
    }    

    @Test(expected = RuntimeException.class)
    public void testWithoutAlternativeUrl() throws Throwable {
        String pluginName = "workflow-api";
        String version = "2.39";
        String nonWorkingConnectionURL = "scm:git:git://github.com/test/workflow-api-plugin.git";
        MavenCoordinates mavenCoordinates = new MavenCoordinates("org.jenkins-ci.plugins", "plugin", "3.54");

        PluginCompatTesterConfig config = new PluginCompatTesterConfig(testFolder.getRoot(),
                new File("../reports/PluginCompatReport.xml"), getSettingsFile());
        config.setIncludePlugins(ImmutableList.of(pluginName));

        PluginCompatTester pct = new PluginCompatTester(config);
        PomData pomData = new PomData(pluginName, "hpi", nonWorkingConnectionURL, pluginName + "-" + version,
                mavenCoordinates, "org.jenkins-ci.plugins.workflow");
        pct.cloneFromSCM(pomData, pluginName, version,
                new File(config.workDirectory.getAbsolutePath() + File.separator + pluginName + File.separator));
    }

    @Test(expected = Test.None.class)
    public void testWithAlternativeUrl() throws Throwable {
        String pluginName = "workflow-api";
        String version = "2.39";
        String nonWorkingConnectionURL = "scm:git:git://github.com/test/workflow-api-plugin.git";
        MavenCoordinates mavenCoordinates = new MavenCoordinates("org.jenkins-ci.plugins", "plugin", "3.54");

        PluginCompatTesterConfig config = new PluginCompatTesterConfig(testFolder.getRoot(),
                new File("../reports/PluginCompatReport.xml"), getSettingsFile());
        config.setIncludePlugins(ImmutableList.of(pluginName));
        config.setFallbackGitHubOrganization("jenkinsci");

        PluginCompatTester pct = new PluginCompatTester(config);
        PomData pomData = new PomData(pluginName, "hpi", nonWorkingConnectionURL, pluginName + "-" + version,
                mavenCoordinates, "org.jenkins-ci.plugins.workflow");
        pct.cloneFromSCM(pomData, pluginName, version,
                new File(config.workDirectory.getAbsolutePath() + File.separator + pluginName + File.separator));
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
