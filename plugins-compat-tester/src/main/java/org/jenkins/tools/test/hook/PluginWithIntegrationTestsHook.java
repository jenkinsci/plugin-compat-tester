package org.jenkins.tools.test.hook;

import java.util.Collection;

import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

/**
 * Hook for plugins with integration tests that need to be executed 
 */
public abstract class PluginWithIntegrationTestsHook extends PluginCompatTesterHookBeforeExecution {

    /** Inform about test type suite to execute integration test (i.e.: failsafe) */
    abstract public Collection<String> getTestTypes();
    
}
