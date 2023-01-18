package org.jenkins.tools.test.hook;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Hook for plugins with integration tests that need to be executed 
 */
public abstract class PluginWithIntegrationTestsHook extends PluginCompatTesterHookBeforeExecution {

    /** Inform about goals to execute integration tests */
    @NonNull
    abstract public Collection<String> getGoals();
    
    /** Inform about test type suite to execute integration test */
    @NonNull
    abstract public Collection<String> getTestTypes();
    
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> action(Map<String, Object> info) {
        List<String> args = (List<String>) info.get("args");

        if (args != null) {
            args.addAll(getGoals());
        }
        
        List<String> types = (List<String>) info.get("types");
        if (types != null) {
            types.addAll(getTestTypes());
        }
        return info;
    }

}
