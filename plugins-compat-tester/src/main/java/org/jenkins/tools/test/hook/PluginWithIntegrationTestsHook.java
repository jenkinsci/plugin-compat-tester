package org.jenkins.tools.test.hook;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

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

    @Override
    public Collection<String> getTestTypes() {
        return Arrays.asList("failsafe");
    }
    
}
