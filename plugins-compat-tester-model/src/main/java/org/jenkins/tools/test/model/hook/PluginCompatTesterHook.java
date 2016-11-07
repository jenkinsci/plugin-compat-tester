package org.jenkins.tools.test.model.hook;

/**
 * The hook interface for creating custom hooks at different points in 
 * the plugin compat tester.
 *
 * Hooks can be called at various points within the process, enabling
 * per-plugin, or all-plugin customization. This includes pom 
 * transformations, adding maven arguments, and other actions that 
 * enable the PluginCompatTester to actually go about testing the plugin
 * rather than throwing up its hands in defeat.
 *
 * This interfact in particular 
 */

 import java.util.Map;

 public interface PluginCompatTesterHook {
     /**
      * Check if the plugin should be affected by this hook.
      * There are several different ways this could be implemented, 
      * and the details are left up to the user.
      */
     public boolean check(Map<String, Object> info) throws Exception;

     //could actually transform pom, return maven args, etc
     /**
      * The core action of what actually needs to be done by the hook.
      * This can do a number of things such as transform the pom,
      * return custom maven args, etc.
      *
      * Certain implementations could throw exceptions.
      */
     public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception;

     /* might not need right now; this was a play around implementation
     public List<String> transformedPlugins();
     */
 }