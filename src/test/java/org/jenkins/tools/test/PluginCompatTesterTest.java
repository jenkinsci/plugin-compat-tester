package org.jenkins.tools.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PluginCompatTesterTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

	@Before
	public void setUp() throws Exception {
		SCMManagerFactory.getInstance().start();
	}
	
	@After
	public void tearDown() throws Exception {
		SCMManagerFactory.getInstance().stop();
	}
	
	@Test
	public void testWithUrl() throws Throwable {
		PluginCompatTester tester = new PluginCompatTester("http://updates.jenkins-ci.org/update-center.json?version=build", "", testFolder.getRoot());
		tester.testPluginAgainst("1.404", "scm-sync-configuration", "http://updates.jenkins-ci.org/download/plugins/scm-sync-configuration/0.0.4/scm-sync-configuration.hpi");
	}
}
