package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.jenkins.tools.test.PluginCompatTester;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

import static org.jenkins.tools.test.PluginCompatTester.SHALLOW_CLONE;

/**
 * Utility class to ease create simple hooks for multimodule projects
 */
public abstract class AbstractMultiParentHook extends PluginCompatTesterHookBeforeCheckout {

    protected boolean firstRun = true;
    
    private PomData pomData;

    @Override
    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        PluginCompatTesterConfig config = (PluginCompatTesterConfig)moreInfo.get("config");
        UpdateSite.Plugin currentPlugin = (UpdateSite.Plugin)moreInfo.get("plugin");


        // We should not execute the hook if using localCheckoutDir
        boolean shouldExecuteHook = config.getLocalCheckoutDir() == null || !config.getLocalCheckoutDir().exists();

        if (shouldExecuteHook) {
            System.out.println("Executing Hook for " + getParentProjectName());
            // Determine if we need to run the download; only run for first identified plugin in the series
            if (firstRun) {
                System.out.println("Preparing for Multimodule checkout");

                // Checkout to the parent directory. All other processes will be on the child directory
                File parentPath = new File(config.workDirectory.getAbsolutePath() + "/" + getParentFolder());

                pomData = (PomData) moreInfo.get("pomData");
                String scmTag;
                if (pomData.getScmTag() != null) {
                    scmTag = pomData.getScmTag();
                    System.out.println(String.format("Using SCM tag '%s' from POM.", scmTag));
                } else {
                    scmTag = getParentProjectName() + "-" + currentPlugin.version;
                    System.out.println(String.format("POM did not provide an SCM tag. Inferring tag '%s'.", scmTag));
                }
                // Like PluginCompatTester.cloneFromSCM but with subdirectories trimmed:
                cloneFromSCM(currentPlugin, parentPath, scmTag, getUrl(), config.getFallbackGitHubOrganization(), moreInfo);
            }

            // Checkout already happened, don't run through again
            moreInfo.put("runCheckout", false);
            firstRun = false;

            // Change the "download"" directory; after download, it's simply used for reference
            File childPath = new File(config.workDirectory.getAbsolutePath() + "/" + getParentFolder() + "/" + getPluginFolderName(currentPlugin));

            System.out.println("Child path for " + currentPlugin.getDisplayName() + " " + childPath);
            moreInfo.put("checkoutDir", childPath);
            moreInfo.put("pluginDir", childPath);
            moreInfo.put("parentFolder", getParentFolder());
        } else {
            configureLocalCheckOut(currentPlugin, config.getLocalCheckoutDir(), moreInfo);
        }

        return moreInfo;
    }

    private void cloneFromSCM(UpdateSite.Plugin currentPlugin, File parentPath, String scmTag, String url, String fallbackGitHubOrganization, Map<String, Object> beforeCheckout)
            throws ComponentLookupException, IOException {
        
        List<String> connectionURLs = new ArrayList<>();
        connectionURLs.add(url);
        if(fallbackGitHubOrganization != null){
            connectionURLs = PluginCompatTester.getFallbackConnectionURL(connectionURLs, url, fallbackGitHubOrganization);
        }
        
        boolean repositoryCloned = false;
        String errorMessage = "";
        for (String connectionURL: connectionURLs){
            if (connectionURL != null) {
                connectionURL = connectionURL.replace("git://", "https://"); // See: https://github.blog/2021-09-01-improving-git-protocol-security-github/
            }
            System.out.println("Checking out from SCM connection URL: " + connectionURL + " (" + getParentProjectName() + "-" + currentPlugin.version + ") at tag " + scmTag);
            if (parentPath.isDirectory()) {
                FileUtils.deleteDirectory(parentPath);
            }
            try {
                boolean result =  PluginCompatTester.clone(connectionURL, scmTag, parentPath, (boolean)beforeCheckout.get(SHALLOW_CLONE));
                if(result) {
                    repositoryCloned = true;
                    break;
                }
            } catch (IOException | InterruptedException e){
                errorMessage = e.getMessage();
            }
        }
        
        if (!repositoryCloned) {
            // Throw an exception if there are any download errors.
            throw new RuntimeException(errorMessage);
        }
    }

    public String getUrl() {
        return pomData.getConnectionUrl().replaceFirst("^(.+github[.]com/[^/]+/[^/]+)/.+", "$1");
    }

    protected void configureLocalCheckOut(UpdateSite.Plugin currentPlugin, File localCheckoutDir, Map<String, Object> moreInfo) {
        // Do nothing to keep compatibility with pre-existing Hooks
        System.out.println("Ignoring localCheckoutDir for " + currentPlugin.getDisplayName());
    }

    /**
     * Returns the folder where the multimodule project parent will be checked out
     */
    protected abstract String getParentFolder();

    /**
     * Returns the parent project name. This will be used to form the checkout tag with the format
     * {@code parentProjectName-version}.
     */
    protected abstract String getParentProjectName();

    /**
     * Returns the plugin folder name. By default it will be the plugin name, but it can be
     * overridden to support plugins (like {@code structs}) that are not located in a folder with
     * the same name as the plugin itself.
     */
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin) {
        return currentPlugin.name;
    }
}
