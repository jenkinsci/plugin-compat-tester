package org.jenkins.tools.test.hook;


import hudson.model.UpdateSite;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.jenkins.tools.test.SCMManagerFactory;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Workaround for the declarative pipeline plugins since they are
 * stored in a central repository.
 *
 */
public class DeclarativePipelineHook extends PluginCompatTesterHookBeforeCheckout {

        private String parentUrl = "scm:git:git://github.com/jenkinsci/pipeline-model-definition-plugin.git";
        private String parentName = "pipeline-model-definition";
        private String parentProjectName = "pipeline-model-definition";
        private List<String> allBundlePlugins = Arrays.asList("pipeline-model-api", "pipeline-model-definition", "pipeline-model-extensions", "pipeline-model-json-shaded", "pipeline-stage-tags-metadata");
        boolean firstRun = true;

        public DeclarativePipelineHook() {}

        /**
         * All the plugins that are part of this repository.
         */
        public List<String> transformedPlugins() {
            return allBundlePlugins;
        }

        /**
         * Perform the checkout from the common repository. Since trying to work this into the PluginCompatTester
         * class pretty much requires a specific rewrite, perform the checkout here and skip the main class.
         */
        public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
            PluginCompatTesterConfig config = (PluginCompatTesterConfig)moreInfo.get("config");
            UpdateSite.Plugin currentPlugin = (UpdateSite.Plugin)moreInfo.get("plugin");
            // We should not execute the hook if using localCheckoutDir
            boolean shouldExecuteHook = config.getLocalCheckoutDir() == null || !config.getLocalCheckoutDir().exists();

            if (shouldExecuteHook) {
                System.out.println("Executing Declarative Pipeline Hook");
                // Determine if we need to run the download; only run for first identified plugin in the series
                if (firstRun) {
                    System.out.println("Preparing for Multimodule checkout");

                    // Checkout to the parent directory. All other processes will be on the child directory
                    File parentPath = new File(config.workDirectory.getAbsolutePath() + "/" + parentName);

                    System.out.println("Checking out from SCM connection URL: " + parentUrl + " (" + parentProjectName + "-" + currentPlugin.version + ")");
                    ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
                    ScmRepository repository = scmManager.makeScmRepository(parentUrl);
                    CheckOutScmResult result = scmManager.checkOut(repository, new ScmFileSet(parentPath), new ScmTag(parentProjectName + "-" + currentPlugin.version));

                    if (!result.isSuccess()) {
                        // Throw an exception if there are any download errors.
                        throw new RuntimeException(result.getProviderMessage() + "||" + result.getCommandOutput());
                    }
                }

                // Checkout already happened, don't run through again
                moreInfo.put("runCheckout", false);
                firstRun = false;

                // Change the "download"" directory; after download, it's simply used for reference
                File childPath = new File(config.workDirectory.getAbsolutePath() + "/" + parentName + "/" + currentPlugin.name);
                System.out.println("Child path for " + currentPlugin.getDisplayName() + " " + childPath);
                moreInfo.put("checkoutDir", childPath);
                moreInfo.put("pluginDir", childPath);
            }

            return moreInfo;
        }
    }
