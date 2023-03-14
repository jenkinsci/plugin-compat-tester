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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.UpdateSite;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.Issue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Main test class for plugin compatibility test frontend
 *
 * @author Frederic Camblor
 */
class PluginCompatTesterTest {

    @Test
    void smokes(@TempDir File tempDir) throws Exception {
        PluginCompatTesterConfig config =
                new PluginCompatTesterConfig(
                        new File("target", "megawar.war").getAbsoluteFile(), tempDir);
        config.setMavenProperties(Map.of("test", "InjectedTest"));
        PluginCompatTester tester = new PluginCompatTester(config);
        tester.testPlugins();
        Path report =
                tempDir.toPath()
                        .resolve("text-finder")
                        .resolve("target")
                        .resolve("surefire-reports")
                        .resolve("TEST-InjectedTest.xml");
        assertTrue(Files.exists(report));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(report.toFile());
        Element element = document.getDocumentElement();
        int tests = Integer.parseInt(element.getAttribute("tests"));
        assertNotEquals(0, tests);
        int errors = Integer.parseInt(element.getAttribute("errors"));
        assertEquals(0, errors);
        int skipped = Integer.parseInt(element.getAttribute("skipped"));
        assertEquals(0, skipped);
        int failures = Integer.parseInt(element.getAttribute("failures"));
        assertEquals(0, failures);
    }

    @Test
    void updateSite() {
        UpdateSite.Data data =
                PluginCompatTester.scanWAR(
                        new File("target", "megawar.war").getAbsoluteFile(),
                        "WEB-INF/(?:optional-)?plugins/([^/.]+)[.][hj]pi");
        assertEquals("core", data.core.name);
        assertNotNull(data.core.version);
        assertEquals("https://foobar", data.core.url);
        UpdateSite.Plugin plugin = data.plugins.get("text-finder");
        assertNotNull(plugin);
        assertEquals("Text Finder", plugin.getDisplayName());
        assertEquals("Text Finder", plugin.title);
        assertEquals("text-finder", plugin.name);
        assertNotNull(plugin.version);
        assertNotNull(plugin.url);
    }

    @Test
    void testMatcher() {

        String fileName = "WEB-INF/lib/jenkins-core-2.7.3-alpha-33.jar";
        Matcher m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-alpha-33", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-ALPHA-33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-ALPHA-33", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-ALPHA-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-ALPHA-33-SNAPSHOT", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-alpha-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-alpha-33-SNAPSHOT", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-beta-33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-beta-33", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-BETA-33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-BETA-33", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-BETA-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-BETA-33-SNAPSHOT", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-BETA-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-BETA-33-SNAPSHOT", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-rc-33", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-RC-33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-RC-33", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-RC-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-RC-33-SNAPSHOT", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-rc-33-SNAPSHOT", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-SNAPSHOT", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-RC33.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-RC33", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-alpha33-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-alpha33-SNAPSHOT", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-rc-SNAPSHOT", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-milestone.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-milestone", m.group(1), "Invalid group");

        fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-milestone.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
        assertEquals("2.7.3-rc-milestone", m.group(1), "Invalid group");
    }

    @Test
    @Issue("JENKINS-50454")
    void testCustomWarPackagerVersions() {
        // TODO: needs more filtering
        String fileName =
                "WEB-INF/lib/jenkins-core-256.0-my-branch-2090468d82e49345519a2457f1d1e7426f01540b-SNAPSHOT.jar";
        Matcher m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");

        fileName =
                "WEB-INF/lib/jenkins-core-256.0-2090468d82e49345519a2457f1d1e7426f01540b-2090468d82e49345519a2457f1d1e7426f01540b-SNAPSHOT.jar";
        m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertTrue(m.matches(), "No matches");
    }

    @Test
    @Issue("340")
    void testJEP229WithUnderscore() {
        String fileName = "WEB-INF/lib/jenkins-core-2.329-rc31964.3b_29e9d46_038_.jar";
        Matcher m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
        assertThat("No matches", m.matches(), is(true));
        assertThat("Invalid group", m.group(1), is("2.329-rc31964.3b_29e9d46_038_"));
    }
}
