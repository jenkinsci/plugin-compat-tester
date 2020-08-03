package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

import java.util.List;
import java.util.Map;

/**
 * Workaround for those plugins with integration tests since they need execute the failsafe:integration-test goal before execution.
 */
public abstract class PluginWithIntegrationTestsHook extends PluginCompatTesterHookBeforeExecution {

    @Override
    public Map<String, Object> action(Map<String, Object> info) throws Exception {
        List<String> args = (List<String>) info.get("args");

        if (args != null) {
            args.add("failsafe:integration-test");
        }

        return info;
    }

}
