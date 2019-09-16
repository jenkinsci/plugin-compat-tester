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

import com.google.common.collect.ImmutableList;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.TestStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Main test class for plugin compatibility test frontend
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
        ImmutableList<String> includedPlugins = ImmutableList.of(
                "accurev"
                /* "active-directory",
                "analysis-collector",
                "scm-sync-configuration" */
        );

        PluginCompatTesterConfig config = new PluginCompatTesterConfig(testFolder.getRoot(),
                new File("../reports/PluginCompatReport.xml"),
                new ClassPathResource("m2-settings.xml").getFile());

		config.setIncludePlugins(includedPlugins);
        config.setSkipTestCache(true);
        config.setCacheThresholStatus(TestStatus.TEST_FAILURES);
        config.setTestCacheTimeout(345600000);
        config.setParentVersion("1.410");
        config.setGenerateHtmlReport(true);

        PluginCompatTester tester = new PluginCompatTester(config);
		tester.testPlugins();
	}

	@Test
	public void testMatcher() {

		String fileName = "WEB-INF/lib/jenkins-core-2.7.3-alpha-33.jar";
		Matcher m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-alpha-33", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-ALPHA-33.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-ALPHA-33", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-ALPHA-33-SNAPSHOT.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-ALPHA-33-SNAPSHOT", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-alpha-33-SNAPSHOT.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-alpha-33-SNAPSHOT", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-beta-33.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-beta-33", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-BETA-33.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-BETA-33", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-BETA-33-SNAPSHOT.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-BETA-33-SNAPSHOT", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-BETA-33-SNAPSHOT.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-BETA-33-SNAPSHOT", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-33.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-rc-33", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-RC-33.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-RC-33", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-RC-33-SNAPSHOT.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-RC-33-SNAPSHOT", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-33-SNAPSHOT.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-rc-33-SNAPSHOT", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-SNAPSHOT.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-SNAPSHOT", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-RC33.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-RC33", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-alpha33-SNAPSHOT.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-alpha33-SNAPSHOT", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-SNAPSHOT.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-rc-SNAPSHOT", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-milestone.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-milestone", m.group(1));

		fileName = "WEB-INF/lib/jenkins-core-2.7.3-rc-milestone.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
		assertEquals("Invalid group", "2.7.3-rc-milestone", m.group(1));	}

	@Test
	@Issue("JENKINS-50454")
	public void testCustomWarPackagerVersions() {
	//TODO: needs more filtering
		String fileName = "WEB-INF/lib/jenkins-core-256.0-my-branch-2090468d82e49345519a2457f1d1e7426f01540b-SNAPSHOT.jar";
		Matcher m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());

		fileName = "WEB-INF/lib/jenkins-core-256.0-2090468d82e49345519a2457f1d1e7426f01540b-2090468d82e49345519a2457f1d1e7426f01540b-SNAPSHOT.jar";
		m = Pattern.compile(PluginCompatTester.JENKINS_CORE_FILE_REGEX).matcher(fileName);
		assertTrue("No matches",m.matches());
	}

}
