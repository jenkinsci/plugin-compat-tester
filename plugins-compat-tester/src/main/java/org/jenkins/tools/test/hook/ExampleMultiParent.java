package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An example hook for dealing with a multiplugin repository. Since the plugin-compat-tester assumes 
 * each plugin is in it's own repository, these plugins automatically fail since they are "not found".
 *
 * This is an example of what needs to change to handle multimodule parents.
 * 
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
        return allBundlePlugins;
    }

    /*
     * No check implementation is required because transformedPlugins
     * returns your specific list.
     */

    /**
     * Point to the shared location.  Check if this needs to be downloaded. 
     */
    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        // Change the url to the location of the parent
        PomData pomData = (PomData)moreInfo.get("pomData");
        pomData.setConnectionUrl(parentUrl);

        // Change the download directory
        PluginCompatTesterConfig config = (PluginCompatTesterConfig)moreInfo.get("config");
        File parentPath = new File(config.workDirectory.getAbsolutePath()+"/"+parentName+"/");
        moreInfo.put("checkoutDir", parentPath);
        
        // Determine if you need to run the download
        // Optional; the PluginCompatTester automatically overwrites old files
        // and you shouldn't break that for new runs.  Implement with caution  
        if(!firstRun){
            moreInfo.put("runCheckout", false);
        }
        firstRun = false;

        return moreInfo;
    }
}