package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite.Plugin;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.jenkins.tools.test.SCMManagerFactory;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

/**
 * An example hook for dealing with a multiplugin repository. Since Plugin Compatibility Tester assumes
 * each plugin is in its own repository, these plugins automatically fail since they are "not
 * found".
 *
 * <p>This is an example of what needs to change to handle multi-module parents.
 */
public class ExampleMultiParent { //extends PluginCompatTesterHookBeforeCheckout {
    private static final Logger LOGGER = Logger.getLogger(ExampleMultiParent.class.getName());

    private String parentUrl = "scm:git:git@github.com:jenkinsci/parent_repo.git";
    private String parentName = "parent_repo";
    private List<String> allBundlePlugins = List.of("possible", "plugins");
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
            LOGGER.log(Level.INFO, "Preparing for multi-module checkout");

            // Checkout to the parent directory. All other processes will be on the child directory
            File parentPath = new File(config.workDirectory.getAbsolutePath()+"/"+parentName);
            
            LOGGER.log(Level.INFO, "Checking out from SCM connection URL: {0} ({1}-{2})", new Object[]{parentUrl, parentName, currentPlugin.version});
            ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
            ScmRepository repository = scmManager.makeScmRepository(parentUrl);
            CheckOutScmResult result = scmManager.checkOut(repository, new ScmFileSet(parentPath), new ScmTag(parentName+"-"+currentPlugin.version));
            
            if(!result.isSuccess()){
                throw new RuntimeException(result.getProviderMessage() + "||" + result.getCommandOutput());
            } 
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