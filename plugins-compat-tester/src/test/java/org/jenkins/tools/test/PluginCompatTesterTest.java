package org.jenkins.tools.test;

import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        List<String> includedPlugins = new ArrayList<String>(){{ /*add("scm-sync-configuration");*/ add("Schmant"); }};

        PluginCompatTesterConfig config = new PluginCompatTesterConfig(testFolder.getRoot(),
                new File(testFolder.getRoot().getAbsolutePath()+"/report.xml"),
                new ClassPathResource("m2-settings.xml").getFile());
		config.setPluginsList(includedPlugins);

        PluginCompatTester tester = new PluginCompatTester(config);
		tester.testPlugins();
	}
}
