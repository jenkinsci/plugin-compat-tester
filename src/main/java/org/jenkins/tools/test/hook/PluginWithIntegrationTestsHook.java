package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collection;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

/** Hook for plugins with integration tests that need to be executed */
public abstract class PluginWithIntegrationTestsHook extends PluginCompatTesterHookBeforeExecution {

    /** Inform about goals to execute integration tests */
    @NonNull
    public abstract Collection<String> getGoals();

    @Override
    public void action(@NonNull BeforeExecutionContext context) {
        context.getArgs().addAll(getGoals());
    }
}
