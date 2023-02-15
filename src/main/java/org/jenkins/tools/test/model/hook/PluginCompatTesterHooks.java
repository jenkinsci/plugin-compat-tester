package org.jenkins.tools.test.model.hook;

import java.io.File;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
    private List<String> hookPrefixes = new ArrayList<>();
    private static Map<String, Map<String, Queue<PluginCompatTesterHook>>> hooksByType =
            new HashMap<>();
    private List<String> excludeHooks = new ArrayList<>();

    /** Create and prepopulate the various hooks for this run of Plugin Compatibility Tester. */
    public PluginCompatTesterHooks() {
        this(new ArrayList<>());
    }

    public PluginCompatTesterHooks(List<String> extraPrefixes) {
        setupPrefixes(extraPrefixes);
        setupHooksByType();
    }

    public PluginCompatTesterHooks(List<String> extraPrefixes, List<File> externalJars) {
        this(extraPrefixes, externalJars, Collections.emptyList());
    }

    public PluginCompatTesterHooks(
            List<String> extraPrefixes, List<File> externalJars, List<String> excludeHooks) {
        setupPrefixes(extraPrefixes);
        setupExternalClassLoaders(externalJars);
        setupHooksByType();
        if (!excludeHooks.isEmpty()) {
            this.excludeHooks.addAll(excludeHooks);
        }
    }

    private void setupHooksByType() {
        for (String stage : List.of("checkout", "execution", "compilation")) {
            hooksByType.put(stage, findHooks(stage));
        }
    }

    private void setupPrefixes(List<String> extraPrefixes) {
        if (!extraPrefixes.isEmpty()) {
            hookPrefixes.addAll(extraPrefixes);
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

    public Map<String, Object> runBeforeCheckout(Map<String, Object> elements)
            throws PluginCompatibilityTesterException {
        return runHooks("checkout", elements);
    }

    public Map<String, Object> runBeforeCompilation(Map<String, Object> elements)
            throws PluginCompatibilityTesterException {
        return runHooks("compilation", elements);
    }

    public Map<String, Object> runBeforeExecution(Map<String, Object> elements)
            throws PluginCompatibilityTesterException {
        return runHooks("execution", elements);
    }

    /**
     * Evaluate and execute hooks for a given stage. There is 1 required object for evaluating any
     * hook: the {@link String} {@code pluginName}.
     *
     * @param stage stage in which to run the hooks
     * @param elements relevant information to hooks at various stages.
     */
    private Map<String, Object> runHooks(String stage, Map<String, Object> elements)
            throws PluginCompatibilityTesterException {
        Queue<PluginCompatTesterHook> beforeHooks = getHooksFromStage(stage, elements);

        // Loop through hooks in a series run in no particular order
        // Modifications build on each other, pertinent checks should be handled in the hook
        for (PluginCompatTesterHook hook : beforeHooks) {
            if (!excludeHooks.contains(hook.getClass().getName()) && hook.check(elements)) {
                LOGGER.log(Level.INFO, "Running hook: {0}", hook.getClass().getName());
                elements = hook.action(elements);
                hook.validate(elements);
            } else {
                LOGGER.log(Level.FINE, "Skipping hook: {0}", hook.getClass().getName());
            }
        }
        return elements;
    }

    public static Queue<PluginCompatTesterHook> getHooksFromStage(
            String stage, Map<String, Object> elements) {
        // List of hooks to execute for the given plugin
        Queue<PluginCompatTesterHook> hooks = new LinkedList<>();

        // Add any hooks that apply for all plugins
        if (hooksByType.get(stage).get("all") != null) {
            hooks.addAll(hooksByType.get(stage).get("all"));
        }

        // Add hooks that applied to this concrete plugin
        String pluginName = (String) elements.get("pluginName");
        if (hooksByType.get(stage).get(pluginName) != null) {
            hooks.addAll(hooksByType.get(stage).get(pluginName));
        }
        return hooks;
    }

    private Map<String, Queue<PluginCompatTesterHook>> findHooks(String stage) {
        Map<String, Queue<PluginCompatTesterHook>> sortedHooks = new HashMap<>();

        // Search for all hooks defined within the given classpath prefix
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addClassLoaders(classLoader);
        for (String hookPrefix : hookPrefixes) {
            configurationBuilder.forPackage(hookPrefix, classLoader);
        }
        Reflections reflections = new Reflections(configurationBuilder);
        Set<Class<? extends PluginCompatTesterHook>> subTypes;

        // Find all steps for a given stage. Long due to casting
        switch (stage) {
            case "compilation":
                Set<Class<? extends PluginCompatTesterHookBeforeCompile>> compSteps =
                        reflections.getSubTypesOf(PluginCompatTesterHookBeforeCompile.class);
                subTypes = compSteps.stream().map(this::casting).collect(Collectors.toSet());
                break;
            case "execution":
                Set<Class<? extends PluginCompatTesterHookBeforeExecution>> exeSteps =
                        reflections.getSubTypesOf(PluginCompatTesterHookBeforeExecution.class);
                subTypes = exeSteps.stream().map(this::casting).collect(Collectors.toSet());
                break;
            case "checkout":
                Set<Class<? extends PluginCompatTesterHookBeforeCheckout>> checkSteps =
                        reflections.getSubTypesOf(PluginCompatTesterHookBeforeCheckout.class);
                subTypes = checkSteps.stream().map(this::casting).collect(Collectors.toSet());
                break;
            default: // Not valid; nothing will get executed
                return new HashMap<>();
        }

        for (Class<?> c : subTypes) {
            // Ignore abstract hooks
            if (!Modifier.isAbstract(c.getModifiers())) {
                try {
                    LOGGER.log(Level.FINE, "Loading hook: {0}", c.getName());
                    Constructor<?> constructor = c.getConstructor();
                    PluginCompatTesterHook hook =
                            (PluginCompatTesterHook) constructor.newInstance();

                    List<String> plugins = hook.transformedPlugins();
                    for (String plugin : plugins) {
                        Queue<PluginCompatTesterHook> allForType = sortedHooks.get(plugin);
                        if (allForType == null) {
                            allForType = new LinkedList<>();
                        }
                        allForType.add(hook);
                        sortedHooks.put(plugin, allForType);
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Error when loading " + c.getName(), e);
                }
            }
        }

        return sortedHooks;
    }

    /**
     * This seems ridiculous, but it is needed to actually convert between the two types of {@link
     * Set}s. Gets around a generics error: {@code incompatible types: inference variable T has
     * incompatible bounds}.
     */
    private Class<? extends PluginCompatTesterHook> casting(
            Class<? extends PluginCompatTesterHook> c) {
        return c;
    }
}
