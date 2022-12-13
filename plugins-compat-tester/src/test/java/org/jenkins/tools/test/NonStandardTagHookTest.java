package org.jenkins.tools.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import hudson.model.UpdateSite;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkins.tools.test.hook.NonStandardTagHook;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.TestStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;

public class NonStandardTagHookTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Test
    public void testConstruction() {
        NonStandardTagHook hook = new NonStandardTagHook();
        List<String> transformedPlugins = hook.transformedPlugins();
        assertNotNull("The list of transformed plugins must be non null", transformedPlugins);
        assertEquals("List of affected plugins must be of size 4", 4, transformedPlugins.size());
        assertTrue("One element in transformedPlugins must be 'artifactID'", transformedPlugins.contains("artifactID"));
    }

    @Test
    public void testCheckMethod() {
        NonStandardTagHook hook = new NonStandardTagHook();
        Map<String, Object> info = new HashMap<>();
        JSONObject pluginData = new JSONObject();
        pluginData.put("name", "electricflow");
        pluginData.put("version", "1.1.8");
        pluginData.put("url", "example.com");
        pluginData.put("dependencies", new JSONArray());
        UpdateSite.Plugin plugin = new UpdateSite("fake", "fake").new Plugin("NO Source", pluginData);
        info.put("plugin", plugin);
        try {
            assertTrue("Check should be true", hook.check(info));
        } catch (Exception e) {
            fail("Exception calling check method " + e.getMessage());
        }
    }

    @Test
    public void testCheckMethodWithMinimumVersion() {
        NonStandardTagHook hook = new NonStandardTagHook();
        Map<String, Object> info = new HashMap<>();
        JSONObject pluginData = new JSONObject();
        pluginData.put("name", "electricflow");
        pluginData.put("version", "1.1.7");
        pluginData.put("url", "example.com");
        pluginData.put("dependencies", new JSONArray());
        UpdateSite.Plugin plugin = new UpdateSite("fake", "fake").new Plugin("NO Source", pluginData);
        info.put("plugin", plugin);
        try {
            assertFalse("Check should be false", hook.check(info));
        } catch (Exception e) {
            fail("Exception calling check method " + e.getMessage());
        }
    }

    @Test
    public void testActuallyPerformsTheCheckoutWithVersionGreaterThanMinimum() {
        try {
            PluginCompatTesterConfig config = new PluginCompatTesterConfig(testFolder.getRoot(),
                    new File("../reports/PluginCompatReport.xml"),
                    new ClassPathResource("m2-settings.xml").getFile());
            config.setIncludePlugins(Collections.singletonList("electricflow"));
            config.setSkipTestCache(true);
            config.setCacheThresholdStatus(TestStatus.TEST_FAILURES);
            config.setTestCacheTimeout(345600000);
            config.setParentVersion("1.410");
            config.setGenerateHtmlReport(true);

            Map<String, Object> info = new HashMap<>();
            PomData data = new PomData("electricflow", "hpi", "scm:git:git://github.com/jenkinsci/electricflow-plugin.git", "cloudbees-flow-1.1.8", new MavenCoordinates("org.jenkins-ci.plugins", "electricflow", "1.1.8"), "org.jenkins-ci.plugins");
            info.put("pomData", data);
            JSONObject pluginData = new JSONObject();
            pluginData.put("name", "electricflow");
            pluginData.put("version", "1.1.8");
            pluginData.put("url", "example.com");
            pluginData.put("dependencies", new JSONArray());
            UpdateSite.Plugin plugin = new UpdateSite("fake", "fake").new Plugin("NO Source", pluginData);
            info.put("plugin", plugin);
            info.put("config", config);

            NonStandardTagHook hook = new NonStandardTagHook();
            Map<String, Object> returnedConfig = hook.action(info);
            assertEquals("RunCheckout should be false", Boolean.FALSE, returnedConfig.get("runCheckout"));
        } catch (Exception e) {
            fail("No exception should be thrown when invoking the hook for valid data but got: " + e);
        }
    }
}
