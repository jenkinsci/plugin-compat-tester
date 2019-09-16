package org.jenkins.tools.test;


import hudson.model.UpdateSite;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.jenkins.tools.test.hook.NonStandardTagHook;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.TestStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
@RunWith(PowerMockRunner.class)
public class NonStandardTagHookTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Test
    public void testConstruction() {
        NonStandardTagHook hook = new NonStandardTagHook();
        List<String> transformedPlugins = hook.transformedPlugins();
        assertNotNull("The list of transformed plugins must be non null", transformedPlugins);
        assertEquals("List of affected plugins must be of size 3", 3, transformedPlugins.size());
        assertEquals("The element in transformedPlugins must be 'artifactID'", "artifactID", transformedPlugins.get(0));
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
    @PrepareForTest({SCMManagerFactory.class,NonStandardTagHook.class})
    public void testActionGeneratesProperInfo() throws Exception {
        spy(SCMManagerFactory.class);
        SCMManagerFactory mockFactory = mock(SCMManagerFactory.class);
        ScmManager mockManager = mock(ScmManager.class);
        CheckOutScmResult mockResult = mock(CheckOutScmResult.class);
        when(mockResult.isSuccess()).thenReturn(Boolean.TRUE);
        when(mockManager.checkOut(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(mockResult);
        when(mockManager.makeScmRepository(Mockito.anyString())).thenReturn(mock(ScmRepository.class));
        when(mockFactory.createScmManager()).thenReturn(mockManager);
        when(SCMManagerFactory.getInstance()).thenReturn(mockFactory);


        Map<String, Object> info = new HashMap<>();
        PomData data = new PomData("artifactID", "hpi", null, null, null);
        info.put("pomData", data);
        JSONObject pluginData = new JSONObject();
        pluginData.put("name","artifactId");
        pluginData.put("version","9.9.99");
        pluginData.put("url", "example.com");
        pluginData.put("dependencies", new JSONArray());
        UpdateSite.Plugin plugin = new UpdateSite("fake", "fake").new Plugin("NO Source",pluginData);
        info.put("plugin", plugin);
        PluginCompatTesterConfig config = new PluginCompatTesterConfig(new File(testFolder.getRoot(), "noexists"), null, null);
        info.put("config", config);

        NonStandardTagHook hook = new NonStandardTagHook();
        Map<String, Object> returnedConfig = hook.action(info);
        assertEquals("Checkout dir is not the expected", new File(testFolder.getRoot(), "noexists/artifactID"), returnedConfig.get("checkoutDir"));
        assertEquals("Checkout dir is not the same as plugin dir", returnedConfig.get("checkoutDir"), returnedConfig.get("pluginDir"));
        assertEquals("RunCheckout should be false", Boolean.FALSE, returnedConfig.get("runCheckout"));

    }

    @Test
    public void testActuallyPerformsTheCheckoutWithVersionGreaterThanMinimum() {

        try {
            SCMManagerFactory.getInstance().start();

            PluginCompatTesterConfig config = new PluginCompatTesterConfig(testFolder.getRoot(),
                    new File("../reports/PluginCompatReport.xml"),
                    new ClassPathResource("m2-settings.xml").getFile());
            config.setIncludePlugins(Collections.singletonList("electricflow"));
            config.setSkipTestCache(true);
            config.setCacheThresholStatus(TestStatus.TEST_FAILURES);
            config.setTestCacheTimeout(345600000);
            config.setParentVersion("1.410");
            config.setGenerateHtmlReport(true);

            Map<String, Object> info = new HashMap<>();
            PomData data = new PomData("electricflow", "hpi", "scm:git:git://github.com/jenkinsci/electricflow-plugin.git", new MavenCoordinates("org.jenkins-ci.plugins", "electricflow", "1.1.8"), "org.jenkins-ci.plugins");
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
            fail("No expection should be thrown when invoking the hook for valid data but got: " + e);
        }
    }
}
