package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite.Plugin;
import org.jenkins.tools.test.model.TestExecutionResult;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Short circuit running any UI plugins that function as a helper methods.
 * These are installed as "plugins" by various parts of the UI, and can't
 * be tested through Maven.
 *
 * Currently UI features are handed through the acceptance test harness.
 * Future work for testing JavaScript? Up to the user.
 *
 * @see <a href="js-libs">https://github.com/jenkinsci/js-libs</a> 
 */
public class SkipUIHelperPlugins extends PluginCompatTesterHookBeforeCheckout {
    private static List<String> allBundlePlugins = Arrays.asList(
        "ace-editor", "bootstrap", "handlebars", "jquery-detached",
        "js-module-base", "momentjs", "numeraljs");

    public SkipUIHelperPlugins() {}

    public List<String> transformedPlugins() {
        return allBundlePlugins;
    }

    /**
     * The plugin was identified as somethig that should be skipped. 
     * Create a TestExecution result preventing forward movement.
     * Also, indicates that we should skip the checkout completely.
     */
    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        moreInfo.put("executionResult", 
            new TestExecutionResult(Arrays.asList("Plugin unsupported at this time, skipping")));
        moreInfo.put("runCheckout", false);
        return moreInfo;
    }
}