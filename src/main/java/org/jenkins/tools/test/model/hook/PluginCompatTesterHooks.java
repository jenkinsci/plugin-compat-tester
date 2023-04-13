package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;

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

    private ClassLoader classLoader = PluginCompatTesterHooks.class.getClassLoader();

    private static final Map<Stage, List<PluginCompatTesterHook<StageContext>>> hooksByStage =
            new EnumMap<>(Stage.class);

    @NonNull
    private final Set<String> excludeHooks;

    public PluginCompatTesterHooks(@NonNull Set<File> externalJars, @NonNull Set<String> excludeHooks) {
        this.excludeHooks = excludeHooks;
        setupExternalClassLoaders(externalJars);
        setupHooksByStage();
    }

    private void setupHooksByStage() {
        hooksByStage.put(Stage.CHECKOUT, findHooks(PluginCompatTesterHookBeforeCheckout.class));
        hooksByStage.put(Stage.COMPILATION, findHooks(PluginCompatTesterHookBeforeCompile.class));
        hooksByStage.put(Stage.EXECUTION, findHooks(PluginCompatTesterHookBeforeExecution.class));
    }

    private void setupExternalClassLoaders(Set<File> externalJars) {
        if (externalJars.isEmpty()) {
            return;
        }
        List<URL> urls = new ArrayList<>();
        for (File jar : externalJars) {
            try {
                urls.add(jar.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new UncheckedIOException(e);
            }
        }
        classLoader = new URLClassLoader(urls.toArray(new URL[0]), classLoader);
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
    private void runHooks(@NonNull StageContext context) throws PluginCompatibilityTesterException {
        for (PluginCompatTesterHook<StageContext> hook : hooksByStage.get(context.getStage())) {
            if (!excludeHooks.contains(hook.getClass().getName()) && hook.check(context)) {
                LOGGER.log(Level.INFO, "Running hook: {0}", hook.getClass().getName());
                hook.action(context);
            } else {
                LOGGER.log(Level.FINE, "Skipping hook: {0}", hook.getClass().getName());
            }
        }
    }

    private List<PluginCompatTesterHook<StageContext>> findHooks(
            Class<? extends PluginCompatTesterHook<? extends StageContext>> clazz) {
        List<PluginCompatTesterHook<StageContext>> sortedHooks = new ArrayList<>();
        for (PluginCompatTesterHook<? extends StageContext> hook : ServiceLoader.load(clazz, classLoader)) {
            sortedHooks.add((PluginCompatTesterHook<StageContext>) hook);
        }
        sortedHooks.sort(new HookOrderComparator());
        return sortedHooks;
    }
}
