package org.jenkins.tools.test.hook;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jenkins.tools.test.model.TestExecutionResult;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

/**
 * Short circuit running any UI plugins that function as a helper methods. These are installed as
 * "plugins" by various parts of the UI and can't be tested through Maven.
 *
 * <p>Currently UI features are handed through the acceptance test harness. Future work for testing
 * JavaScript? Up to the user.
 *
 * @see <a href="js-libs">https://github.com/jenkinsci/js-libs</a>
 */
public class SkipUIHelperPlugins extends PluginCompatTesterHookBeforeCheckout {
    private static List<String> allBundlePlugins = List.of(
        "ace-editor", "bootstrap", "handlebars", "jquery-detached",
        "js-module-base", "momentjs", "numeraljs");

    public SkipUIHelperPlugins() {}

    @Override
    public List<String> transformedPlugins() {
        return Collections.unmodifiableList(allBundlePlugins);
    }

    /**
     * The plugin was identified as something that should be skipped. Create a {@link TestExecutionResult}
     * preventing forward movement. Also, indicates that we should skip the checkout completely.
     */
    @Override
    public Map<String, Object> action(Map<String, Object> moreInfo) {
        moreInfo.put("executionResult", 
            new TestExecutionResult(List.of("Plugin unsupported at this time, skipping")));
        moreInfo.put("runCheckout", false);
        return moreInfo;
    }
}