package org.jenkins.tools.test.hook;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Workaround for those plugins with integration tests since they need execute the failsafe:integration-test goal before execution.
 */
public class PluginWithFailsafeIntegrationTestsHook extends PluginWithIntegrationTestsHook {

    @Override
    public Collection<String> getGoals() {
        return Arrays.asList("failsafe:integration-test");
    }

    @Override
    public Collection<String> getTestTypes() {
        return Arrays.asList("failsafe");
    }
    
}
