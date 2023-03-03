package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginCompatTesterHookBeforeCheckout.class)
public class WarningsNGCheckoutHook extends AbstractMultiParentHook {

    private static final Logger LOGGER = Logger.getLogger(WarningsNGCheckoutHook.class.getName());

    @Override
    protected String getParentFolder() {
        return "warnings-ng-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        Model model = context.getModel();
        return "warnings-ng-parent".equals(model.getArtifactId()) // localCheckoutDir
                || "warnings-ng".equals(model.getArtifactId()); // checkout
    }

    @Override
    protected String getPluginFolderName(@NonNull BeforeCheckoutContext context) {
        return "plugin";
    }

    @Override
    protected void configureLocalCheckOut(
            File localCheckoutDir, @NonNull BeforeCheckoutContext context) {
        File pluginDir = new File(localCheckoutDir, getPluginFolderName(context));
        if (!pluginDir.exists() && !pluginDir.isDirectory()) {
            throw new RuntimeException(
                    "Invalid localCheckoutDir for " + context.getPlugin().getDisplayName());
        }

        // Checkout already happened, don't run through again
        context.setRanCheckout(true);
        firstRun = false;

        // Change the "download"" directory; after download, it's simply used for reference
        LOGGER.log(
                Level.INFO,
                "Child path for {0}: {1}",
                new Object[] {context.getPlugin().getDisplayName(), pluginDir.getPath()});
        context.setCheckoutDir(pluginDir);
        context.setPluginDir(pluginDir);
    }
}
