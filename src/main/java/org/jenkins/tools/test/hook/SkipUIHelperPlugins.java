package org.jenkins.tools.test.hook;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final Logger LOGGER = Logger.getLogger(SkipUIHelperPlugins.class.getName());

    private static List<String> allBundlePlugins =
            List.of(
                    "ace-editor",
                    "bootstrap",
                    "handlebars",
                    "jquery-detached",
                    "js-module-base",
                    "momentjs",
                    "numeraljs");

    @Override
    public List<String> transformedPlugins() {
        return Collections.unmodifiableList(allBundlePlugins);
    }

    @Override
    public Map<String, Object> action(Map<String, Object> moreInfo) {
        LOGGER.log(Level.WARNING, "Plugin unsupported at this time, skipping");
        moreInfo.put("skipPlugin", true);
        return moreInfo;
    }
}
