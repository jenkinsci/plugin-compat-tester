package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.PluginCompatTester;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

/** Utility class to ease create simple hooks for multi-module projects */
public abstract class AbstractMultiParentHook extends PluginCompatTesterHookBeforeCheckout {

    private static final Logger LOGGER = Logger.getLogger(AbstractMultiParentHook.class.getName());

    protected boolean firstRun = true;

    @Override
    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "intended behavior")
    public void action(@NonNull BeforeCheckoutContext context)
            throws PluginSourcesUnavailableException {

        // We should not execute the hook if using localCheckoutDir
        File localCheckoutDir = context.getConfig().getLocalCheckoutDir();
        boolean shouldExecuteHook = localCheckoutDir == null || !localCheckoutDir.exists();

        if (shouldExecuteHook) {
            LOGGER.log(Level.INFO, "Executing hook for {0}", context.getPlugin().getDisplayName());
            // Determine if we need to run the download; only run for first identified plugin in the
            // series
            if (firstRun) {
                LOGGER.log(Level.INFO, "Preparing for multi-module checkout");

                // Checkout to the parent directory. All other processes will be on the child
                // directory
                File parentPath =
                        new File(
                                context.getConfig().getWorkingDir().getAbsolutePath()
                                        + "/"
                                        + getParentFolder());

                Model model = context.getModel();
                // Like the call in PluginCompatTester#runHooks but with subdirectories trimmed:
                PluginCompatTester.cloneFromScm(
                        model.getScm().getConnection(),
                        context.getConfig().getFallbackGitHubOrganization(),
                        model.getScm().getTag(),
                        parentPath);
            }

            // Checkout already happened, don't run through again
            context.setRanCheckout(true);
            firstRun = false;

            // Change the "download"" directory; after download, it's simply used for reference
            File childPath =
                    new File(
                            context.getConfig().getWorkingDir().getAbsolutePath()
                                    + "/"
                                    + getParentFolder()
                                    + "/"
                                    + getPluginFolderName(context));

            LOGGER.log(
                    Level.INFO,
                    "Child path for {0}: {1}",
                    new Object[] {context.getPlugin().getDisplayName(), childPath.getPath()});
            context.setCheckoutDir(childPath);
            context.setPluginDir(childPath);
            context.setParentFolder(getParentFolder());
        } else {
            configureLocalCheckOut(context.getConfig().getLocalCheckoutDir(), context);
        }
    }

    protected void configureLocalCheckOut(
            File localCheckoutDir, @NonNull BeforeCheckoutContext context)
            throws PluginSourcesUnavailableException {

        File pluginDir = new File(localCheckoutDir, getPluginFolderName(context));
        if (!pluginDir.exists() && !pluginDir.isDirectory()) {
            throw new PluginSourcesUnavailableException(
                    "Invalid localCheckoutDir for " + context.getPlugin().getDisplayName());
        }
        // behave exactly as if we have cloned so other hooks do not need to handle the difference
        // between a localcheckout and a fresh clone
        context.setRanCheckout(true);
        context.setCheckoutDir(pluginDir);
        // context.setParentFolder("."); // it won;t be in a subdirectory like we had a clone
        context.setPluginDir(pluginDir);
    }

    /**
     * Return the folder where the multi-module project will be checked out. This should be the name
     * of the plugin's Git repository.
     */
    protected abstract String getParentFolder();

    /**
     * Returns the plugin folder name. By default it will be the plugin name, but it can be
     * overridden to support plugins (like {@code workflow-cps}) that are not located in a folder
     * with the same name as the plugin itself.
     */
    protected String getPluginFolderName(@NonNull BeforeCheckoutContext context) {
        return context.getPlugin().name;
    }
}
