package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins.tools.test.PluginCompatTester;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

/** Utility class to ease create simple hooks for multi-module projects */
public abstract class AbstractMultiParentHook extends PluginCompatTesterHookBeforeCheckout {

    private static final Logger LOGGER = Logger.getLogger(AbstractMultiParentHook.class.getName());

    protected boolean firstRun = true;

    private PomData pomData;

    @Override
    public Map<String, Object> action(Map<String, Object> moreInfo)
            throws PluginSourcesUnavailableException {
        PluginCompatTesterConfig config = (PluginCompatTesterConfig) moreInfo.get("config");
        UpdateSite.Plugin currentPlugin = (UpdateSite.Plugin) moreInfo.get("plugin");

        // We should not execute the hook if using localCheckoutDir
        File localCheckoutDir = config.getLocalCheckoutDir();
        boolean shouldExecuteHook = localCheckoutDir == null || !localCheckoutDir.exists();

        if (shouldExecuteHook) {
            LOGGER.log(Level.INFO, "Executing hook for {0}", getParentProjectName());
            // Determine if we need to run the download; only run for first identified plugin in the
            // series
            if (firstRun) {
                LOGGER.log(Level.INFO, "Preparing for multi-module checkout");

                // Checkout to the parent directory. All other processes will be on the child
                // directory
                File parentPath =
                        new File(
                                config.getWorkingDir().getAbsolutePath() + "/" + getParentFolder());

                pomData = (PomData) moreInfo.get("pomData");
                // Like the call in PluginCompatTester#runHooks but with subdirectories trimmed:
                PluginCompatTester.cloneFromScm(
                        getUrl(),
                        config.getFallbackGitHubOrganization(),
                        PluginCompatTester.getScmTag(
                                pomData, getParentProjectName(), currentPlugin.version),
                        parentPath);
            }

            // Checkout already happened, don't run through again
            moreInfo.put("runCheckout", false);
            firstRun = false;

            // Change the "download"" directory; after download, it's simply used for reference
            File childPath =
                    new File(
                            config.getWorkingDir().getAbsolutePath()
                                    + "/"
                                    + getParentFolder()
                                    + "/"
                                    + getPluginFolderName(currentPlugin));

            LOGGER.log(
                    Level.INFO,
                    "Child path for {0}: {1}",
                    new Object[] {currentPlugin.getDisplayName(), childPath.getPath()});
            moreInfo.put("checkoutDir", childPath);
            moreInfo.put("pluginDir", childPath);
            moreInfo.put("parentFolder", getParentFolder());
        } else {
            configureLocalCheckOut(currentPlugin, config.getLocalCheckoutDir(), moreInfo);
        }

        return moreInfo;
    }

    public String getUrl() {
        return pomData.getConnectionUrl().replaceFirst("^(.+github[.]com/[^/]+/[^/]+)/.+", "$1");
    }

    protected void configureLocalCheckOut(
            UpdateSite.Plugin currentPlugin, File localCheckoutDir, Map<String, Object> moreInfo) {
        // Do nothing to keep compatibility with pre-existing Hooks
        LOGGER.log(
                Level.INFO,
                "Ignoring local checkout directory for {0}",
                currentPlugin.getDisplayName());
    }

    /**
     * Return the folder where the multi-module project will be checked out. This should be the name
     * of the plugin's Git repository.
     */
    protected abstract String getParentFolder();

    /**
     * Return the prefix to the SCM tag (usually the artifact ID of the base module). This will be
     * used to form the checkout tag with the format {@code parentProjectName-version} in the
     * (highly unlikely, and impossible for incrementalified plugins) event that the SCM tag is
     * missing from the plugin's POM.
     */
    protected abstract String getParentProjectName();

    /**
     * Returns the plugin folder name. By default it will be the plugin name, but it can be
     * overridden to support plugins (like {@code workflow-cps}) that are not located in a folder
     * with the same name as the plugin itself.
     */
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin) {
        return currentPlugin.name;
    }
}
