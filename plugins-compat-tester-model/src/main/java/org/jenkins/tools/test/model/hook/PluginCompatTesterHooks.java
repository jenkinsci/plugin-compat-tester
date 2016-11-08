package org.jenkins.tools.test.model.hook;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.lang.reflect.Constructor;

/**
 * Loads and executes hooks for modifying the state of the Plugin Compat Tester at different 
 * points along the setup.  Hooks extec a particular subclass defining their type and 
 * implement the PluginCompatTesterHook interface.
 *
 * Note: the classes will not have a parameterized constructor or at least that 
 * constructor will never be called
 */

public class PluginCompatTesterHooks {
    public static Map<String, Object> runBeforeCheckout(Map<String, Object> elements) {
        return runHooks("checkout", elements);
    }

    /**
     * This might not be incredibly useful; most precompilation can be caught by the checkout
     * hook, and the point is to get as close to actual state of the 
     */
    public static Map<String, Object> runBeforeCompilation(Map<String, Object> elements) {
        return runHooks("compilation", elements);
    }
    
    public static Map<String, Object> runBeforeExecution(Map<String, Object> elements) {
        return runHooks("execution", elements);
    }

    private static Map<String, Object> runHooks(String stage, Map<String, Object> elements) throws RuntimeException {
        Queue<String> beforeHooks = findHooks(stage); 

        // Loop through hooks in a series run in no particular order
        // Modifications build on each other, pertenent checks should be handled in the hook
        for(String hook : beforeHooks) {
            try {
                System.out.println("Processing " + hook);
                Class<?> clazz = Class.forName(hook);
                Constructor<?> constructor = clazz.getConstructor();
                PluginCompatTesterHook instance = (PluginCompatTesterHook)constructor.newInstance();

                if(instance.check(elements)) {
                    elements = instance.action(elements);
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

    /**
     * Identify all the classes for a given stage of execution.
     *
     * TODO: want to build more of a dynamic system; I don't really want to use reflection...
     * Though, since I'm already using reflection with actually loading the class, I'll probably just go down that route
     */
    private static Queue<String> findHooks(String stage) {
        Queue<String> allForType = new LinkedList<String>();
        switch(stage) {
            case "compilation" : 
                break;
            case "execution" : 
                allForType.add("org.jenkins.tools.test.hook.TransformPom");
                break;
            case "checkout" : 
                break;
            default:
        }

        return allForType;
    }
}