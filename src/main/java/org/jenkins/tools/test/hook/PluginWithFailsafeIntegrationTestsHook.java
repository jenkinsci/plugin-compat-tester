package org.jenkins.tools.test.hook;

import java.util.Collection;
import java.util.List;

/**
 * Workaround for those plugins with integration tests since they need execute the
 * failsafe:integration-test goal before execution.
 */
public abstract class PluginWithFailsafeIntegrationTestsHook extends PluginWithIntegrationTestsHook {

    @Override
    public Collection<String> getGoals() {
        return List.of("failsafe:integration-test");
    }
}
