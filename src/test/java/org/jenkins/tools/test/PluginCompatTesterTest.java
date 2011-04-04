package org.jenkins.tools.test;

import org.junit.Test;

public class PluginCompatTesterTest {

	@Test
	public void testWithUrl(){
		PluginCompatTester tester = new PluginCompatTester("http://updates.jenkins-ci.org/update-center.json?version=build", "");
		tester.testPluginAgainst("1.404", "http://updates.jenkins-ci.org/download/plugins/scm-sync-configuration/0.0.4/scm-sync-configuration.hpi");
	}
}
