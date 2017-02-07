package com.cloudbees.pct.hook;
import hudson.model.UpdateSite.Plugin;
import org.jenkins.tools.test.model.TestExecutionResult;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 
 * How does structuring a plugin like this work in the grand scheme of things?
 * Let's discover how maven loading works plus some other stuff for fun.
 */

public class ExternalHook extends PluginCompatTesterHookBeforeCheckout {
    /*private final List<String> allBundlePlugins = Arrays.asList(
        "ace-editor", "bootstrap", "handlebars", "jquery-detached",
        "js-module-base", "momentjs", "numeraljs");*/

    public ExternalHook() {}

    public List<String> transformedPlugins() {
        return Arrays.asList(
        "ace-editor", "bootstrap", "handlebars", "jquery-detached",
        "js-module-base", "momentjs", "numeraljs");
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