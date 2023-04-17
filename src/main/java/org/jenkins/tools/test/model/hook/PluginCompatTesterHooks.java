package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.util.ServiceHelper;

/**
 * Loads and executes hooks for modifying the state of Plugin Compatibility Tester at different
 * points along the setup. Hooks extend a particular subclass defining their type and implement the
 * {@link PluginCompatTesterHook} interface.
 *
 * <p>Note: the classes will not have a parameterized constructor or at least that constructor will
 * never be called.
 */
public class PluginCompatTesterHooks {
    private static final Logger LOGGER = Logger.getLogger(PluginCompatTesterHooks.class.getName());

    private static final Map<Stage, List<? extends PluginCompatTesterHook<? extends StageContext>>> hooksByStage =
            new EnumMap<>(Stage.class);

    @NonNull
    private final Set<String> excludeHooks;

    public PluginCompatTesterHooks(@NonNull ServiceHelper serviceHelper, @NonNull Set<String> excludeHooks) {
        this.excludeHooks = excludeHooks;
        setupHooksByStage(serviceHelper);
    }

    private void setupHooksByStage(@NonNull ServiceHelper serviceHelper) {
        hooksByStage.put(Stage.CHECKOUT, serviceHelper.loadServices(PluginCompatTesterHookBeforeCheckout.class));
        hooksByStage.put(Stage.COMPILATION, serviceHelper.loadServices(PluginCompatTesterHookBeforeCompile.class));
        hooksByStage.put(Stage.EXECUTION, serviceHelper.loadServices(PluginCompatTesterHookBeforeExecution.class));
    }

    public void runBeforeCheckout(@NonNull BeforeCheckoutContext context) throws PluginCompatibilityTesterException {
        runHooks(context);
    }

    public void runBeforeCompilation(@NonNull BeforeCompilationContext context)
            throws PluginCompatibilityTesterException {
        runHooks(context);
    }

    public void runBeforeExecution(@NonNull BeforeExecutionContext context) throws PluginCompatibilityTesterException {
        runHooks(context);
    }

    /**
     * Evaluate and execute hooks for a given stage. There is 1 required object for evaluating any
     * hook: the {@link String} {@code pluginName}.
     *
     * @param context relevant information to hooks at various stages.
     */
    private <C extends StageContext> void runHooks(@NonNull C context) throws PluginCompatibilityTesterException {
        for (PluginCompatTesterHook<C> hook :
                (List<? extends PluginCompatTesterHook<C>>) hooksByStage.get(context.getStage())) {
            if (!excludeHooks.contains(hook.getClass().getName()) && hook.check(context)) {
                LOGGER.log(Level.INFO, "Running hook: {0}", hook.getClass().getName());
                hook.action(context);
            } else {
                LOGGER.log(Level.FINE, "Skipping hook: {0}", hook.getClass().getName());
            }
        }
    }
}
