package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite.Plugin;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jenkins.tools.test.PluginCompatTester;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

/**
 * An example hook for dealing with a multiplugin repository. Since Plugin Compatibility Tester assumes
 * each plugin is in its own repository, these plugins automatically fail since they are "not
 * found".
 *
 * <p>This is an example of what needs to change to handle multimodule parents.
 */
public class ExampleMultiParent { //extends PluginCompatTesterHookBeforeCheckout {
    private String parentUrl = "scm:git:git@github.com:jenkinsci/parent_repo.git";
    private String parentName = "parent_repo";
    private List<String> allBundlePlugins = Arrays.asList("possible", "plugins");
    boolean firstRun = true;

    public ExampleMultiParent() {}

    /**
     * All the plugins that are part of this repository.
     */
    public List<String> transformedPlugins() {
        return Collections.unmodifiableList(allBundlePlugins);
    }

    /*
     * No check implementation is required because transformedPlugins
     * returns your specific list.
     */

    /**
     * Point to the shared location.  Check if this needs to be downloaded. 
     */
    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        PluginCompatTesterConfig config = (PluginCompatTesterConfig)moreInfo.get("config");
        Plugin currentPlugin = (Plugin)moreInfo.get("plugin");

        // Determine if we need to run the download; only run for first identified plugin in the series
        if(firstRun){
            System.out.println("Preparing for Multimodule checkout.");

            // Checkout to the parent directory. All other processes will be on the child directory
            File parentPath = new File(config.workDirectory.getAbsolutePath()+"/"+parentName);
            
            PluginCompatTester.clone(parentUrl, parentName + "-" + currentPlugin.version, parentPath);
        }

        // Checkout already happened, don't run through again
        moreInfo.put("runCheckout", false);
        firstRun = false;

        // Change the "download"" directory; after download, it's simply used for reference
        File childPath = new File(config.workDirectory.getAbsolutePath()+"/"+parentName+"/"+currentPlugin.name);
        moreInfo.put("checkoutDir", childPath);

        return moreInfo;
    }
}