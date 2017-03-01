package org.jenkins.tools.test.model.hook;

import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads and executes hooks for modifying the state of the Plugin Compat Tester at different 
 * points along the setup.  Hooks extec a particular subclass defining their type and 
 * implement the PluginCompatTesterHook interface.
 *
 * Note: the classes will not have a parameterized constructor or at least that 
 * constructor will never be called
 */

public class PluginCompatTesterHooks {
    private List<String> hookPrefixes = new ArrayList<String>();
    private Map<String, Map<String, Queue<PluginCompatTesterHook>>> hooksByType = new HashMap<String, Map<String, Queue<PluginCompatTesterHook>>>();

    /**
     * Create and prepopulate the various hooks for this run of the plugin-compat-tester.
     * 
     */
    public PluginCompatTesterHooks() {
        this(new ArrayList<String>());
    }
    public PluginCompatTesterHooks(List<String> extraPrefixes) {
        if(extraPrefixes != null) {
            hookPrefixes.addAll(extraPrefixes);
        }
        for(String stage : Arrays.asList("checkout", "execution", "compilation")) {
            hooksByType.put(stage, findHooks(stage));
        }
    }

    public Map<String, Object> runBeforeCheckout(Map<String, Object> elements) {
        return runHooks("checkout", elements);
    }

    public Map<String, Object> runBeforeCompilation(Map<String, Object> elements) {
        return runHooks("compilation", elements);
    }
    
    public Map<String, Object> runBeforeExecution(Map<String, Object> elements) {
        return runHooks("execution", elements);
    }

    /**
     * Evaluate and execute hooks for a given stage.  There is 1 required object for evaluting any
     * hook.  [String, "pluginName"] 
     *
     * @param stage    stage which 
     * @param elements relevant information to hooks at various stages.
     */
    private Map<String, Object> runHooks(String stage, Map<String, Object> elements) throws RuntimeException {
        String pluginName = (String)elements.get("pluginName");
        Queue<PluginCompatTesterHook> beforeHooks = hooksByType.get(stage).get("all") != null ? hooksByType.get(stage).get("all") : new LinkedList<PluginCompatTesterHook>();
        if(hooksByType.get(stage).get(pluginName) != null) {
            beforeHooks.addAll(hooksByType.get(stage).get(pluginName));
        }
        
        // Loop through hooks in a series run in no particular order
        // Modifications build on each other, pertenent checks should be handled in the hook
        for(PluginCompatTesterHook hook : beforeHooks) {
            try {
                System.out.println("Processing " + hook.getClass().getName());
                if(hook.check(elements)) {
                    elements = hook.action(elements);
                    hook.validate(elements);
                } else {
                    System.out.println("Hook not triggered.  Continuing.");
                }
            } catch (RuntimeException re) {
                //this type of exception should stop processing the plugins. Throw it up the chain
                throw re;
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Cannot make transformation; continue.");
            }
        }
        return elements;
    }

    private Map<String, Queue<PluginCompatTesterHook>> findHooks(String stage) {
        Map<String, Queue<PluginCompatTesterHook>> sortedHooks = new HashMap<String, Queue<PluginCompatTesterHook>>();
        
        // Search for all hooks defined within the given classpath prefix
        for(String prefix : hookPrefixes) {
            Reflections reflections = new Reflections(prefix);
            Set<Class<? extends PluginCompatTesterHook>> subTypes;
            
            // Find all steps for a given stage. Long due to casting
            switch(stage) {
                case "compilation" : 
                    Set<Class<? extends PluginCompatTesterHookBeforeCompile>> compSteps = reflections.getSubTypesOf(PluginCompatTesterHookBeforeCompile.class); 
                    subTypes = compSteps.stream()
                        .map(elt -> casting(elt))
                        .collect(Collectors.toSet());
                    break;
                case "execution" : 
                    Set<Class<? extends PluginCompatTesterHookBeforeExecution>> exeSteps = reflections.getSubTypesOf(PluginCompatTesterHookBeforeExecution.class); 
                    subTypes = exeSteps.stream()
                        .map(elt -> casting(elt))
                        .collect(Collectors.toSet());
                    break;
                case "checkout" : 
                    Set<Class<? extends PluginCompatTesterHookBeforeCheckout>> checkSteps = reflections.getSubTypesOf(PluginCompatTesterHookBeforeCheckout.class); 
                    subTypes = checkSteps.stream()
                        .map(elt -> casting(elt))
                        .collect(Collectors.toSet());
                    break;
                default: // Not valid; nothing will get executed
                    return new HashMap<String, Queue<PluginCompatTesterHook>>();
            }
            
            for(Class c : subTypes) {
                try {
                    System.out.println("Hook: " + c.getName());
                    Constructor<?> constructor = c.getConstructor();
                    PluginCompatTesterHook hook = (PluginCompatTesterHook)constructor.newInstance();

                    List<String> plugins = hook.transformedPlugins();
                    for(String plugin : plugins) {
                        Queue<PluginCompatTesterHook> allForType = sortedHooks.get(plugin);
                        if(allForType == null){
                            allForType = new LinkedList<PluginCompatTesterHook>();
                        }
                        allForType.add(hook);
                        sortedHooks.put(plugin, allForType);
                    }
                } catch (Exception ex) {
                    System.out.println("Error when loading " + c.getName());
                    ex.printStackTrace();
                    continue;
                }
            }
        }
        return sortedHooks;
    }

    /**
     * Seems rediculous, but is needed to actually convert between the two types of Sets.
     * Gets around generics error: {@code incompatible types: inference variable T has incompatible bounds}
     */
    private Class<? extends PluginCompatTesterHook> casting(Class<? extends PluginCompatTesterHook> c) {
        return (Class<? extends PluginCompatTesterHook>)c;
    }
}