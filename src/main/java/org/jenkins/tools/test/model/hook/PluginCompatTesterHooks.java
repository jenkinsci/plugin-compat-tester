package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

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

    @NonNull private final List<String> hookPrefixes;

    public static final Map<Stage, List<PluginCompatTesterHook<StageContext>>> hooksByStage =
            new EnumMap<>(Stage.class);

    @NonNull private final List<String> excludeHooks;

    public PluginCompatTesterHooks(
            @NonNull List<String> extraPrefixes,
            @NonNull List<File> externalJars,
            @NonNull List<String> excludeHooks) {
        this.hookPrefixes = extraPrefixes;
        this.excludeHooks = excludeHooks;
        setupExternalClassLoaders(externalJars);
        setupHooksByStage();
    }

    private void setupHooksByStage() {
        for (Stage stage : Stage.values()) {
            hooksByStage.put(stage, findHooks(stage));
        }
    }

    private void setupExternalClassLoaders(List<File> externalJars) {
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

    public void runBeforeCheckout(@NonNull BeforeCheckoutContext context)
            throws PluginCompatibilityTesterException {
        runHooks(context);
    }

    public void runBeforeCompilation(@NonNull BeforeCompilationContext context)
            throws PluginCompatibilityTesterException {
        runHooks(context);
    }

    public void runBeforeExecution(@NonNull BeforeExecutionContext context)
            throws PluginCompatibilityTesterException {
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

    private List<PluginCompatTesterHook<StageContext>> findHooks(Stage stage) {
        List<PluginCompatTesterHook<StageContext>> sortedHooks = new ArrayList<>();

        // Search for all hooks defined within the given classpath prefix
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addClassLoaders(classLoader);
        for (String hookPrefix : hookPrefixes) {
            configurationBuilder.forPackage(hookPrefix, classLoader);
        }
        Reflections reflections = new Reflections(configurationBuilder);
        NavigableSet<Class<? extends PluginCompatTesterHook<? extends StageContext>>> subTypes;

        // Find all steps for a given stage. Long due to casting
        switch (stage) {
            case COMPILATION:
                Set<Class<? extends PluginCompatTesterHookBeforeCompile>> compSteps =
                        reflections.getSubTypesOf(PluginCompatTesterHookBeforeCompile.class);
                subTypes =
                        compSteps.stream()
                                .map(this::casting)
                                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                                .collect(Collectors.toCollection(navigableSet()));
                break;
            case EXECUTION:
                Set<Class<? extends PluginCompatTesterHookBeforeExecution>> exeSteps =
                        reflections.getSubTypesOf(PluginCompatTesterHookBeforeExecution.class);
                subTypes =
                        exeSteps.stream()
                                .map(this::casting)
                                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                                .collect(Collectors.toCollection(navigableSet()));
                break;
            case CHECKOUT:
                Set<Class<? extends PluginCompatTesterHookBeforeCheckout>> checkSteps =
                        reflections.getSubTypesOf(PluginCompatTesterHookBeforeCheckout.class);
                subTypes =
                        checkSteps.stream()
                                .map(this::casting)
                                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                                .collect(Collectors.toCollection(navigableSet()));
                break;
            default: // Not valid; nothing will get executed
                throw new IllegalArgumentException("Invalid stage: " + stage);
        }

        for (Class<? extends PluginCompatTesterHook<? extends StageContext>> c : subTypes) {
            try {
                LOGGER.log(Level.FINE, "Loading hook: {0}", c.getName());
                Constructor<? extends PluginCompatTesterHook<? extends StageContext>> constructor =
                        c.getConstructor();
                PluginCompatTesterHook<StageContext> hook =
                        (PluginCompatTesterHook<StageContext>) constructor.newInstance();
                sortedHooks.add(hook);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Error when loading " + c.getName(), e);
            }
        }

        return sortedHooks;
    }

    private static Supplier<
                    NavigableSet<Class<? extends PluginCompatTesterHook<? extends StageContext>>>>
            navigableSet() {
        return () -> new TreeSet<>(Comparator.comparing(Class::getName));
    }

    /**
     * This seems ridiculous, but it is needed to actually convert between the two types of {@link
     * Set}s. Gets around a generics error: {@code incompatible types: inference variable T has
     * incompatible bounds}.
     */
    private Class<? extends PluginCompatTesterHook<? extends StageContext>> casting(
            Class<? extends PluginCompatTesterHook<? extends StageContext>> c) {
        return c;
    }
}
