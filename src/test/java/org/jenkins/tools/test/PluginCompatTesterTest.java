package org.jenkins.tools.test;

import org.junit.Test;

public class PluginCompatTesterTest {

	@Test
	public void testWithUrl(){
		PluginCompatTester tester = new PluginCompatTester("http://updates.jenkins-ci.org/update-center.json?version=build", "");
		tester.testPlugins();
	}
}
