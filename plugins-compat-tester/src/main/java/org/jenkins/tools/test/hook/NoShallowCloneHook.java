package org.jenkins.tools.test.hook;

import org.apache.commons.lang.StringUtils;
import org.jenkins.tools.test.PluginCompatTester;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

import java.util.Map;

public class NoShallowCloneHook extends PluginCompatTesterHookBeforeCheckout {

    @Override
    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        String pluginName = (String) moreInfo.get("pluginName");
        // such plugin need commit history for some tests
        if (StringUtils.startsWith(pluginName, "git")) {
            System.out.println("Disable shallow clone for plugin:" + pluginName);
            moreInfo.put(PluginCompatTester.SHALLOW_CLONE, false);
        }
        return moreInfo;
    }
}
