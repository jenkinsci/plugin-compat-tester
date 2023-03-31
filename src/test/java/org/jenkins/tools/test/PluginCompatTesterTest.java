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
                        .resolve("text-finder-plugin")
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
    void testDirectoryFromGitUrl() throws Exception {
        assertEquals(
                "plugin-compat-tester",
                PluginCompatTester.getRepoNameFromGitURL(
                        "ssh://git@github.com/jenkinsci/plugin-compat-tester.git"));
        assertEquals(
                "plugin-compat-tester",
                PluginCompatTester.getRepoNameFromGitURL(
                        "https://github.com/jenkinsci/plugin-compat-tester.git"));
        assertEquals(
                "plugin-compat-tester",
                PluginCompatTester.getRepoNameFromGitURL(
                        "git@host.xz:jenkinsci/plugin-compat-tester.git"));
        assertEquals(
                "plugin-compat-tester",
                PluginCompatTester.getRepoNameFromGitURL(
                        "ssh://git@github.com/jenkinsci/plugin-compat-tester"));
        assertEquals(
                "plugin-compat-tester",
                PluginCompatTester.getRepoNameFromGitURL(
                        "https://github.com/jenkinsci/plugin-compat-tester"));
        assertEquals(
                "plugin-compat-tester",
                PluginCompatTester.getRepoNameFromGitURL(
                        "git@host.xz:jenkinsci/plugin-compat-tester"));
    }
}
