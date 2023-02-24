package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

public class WarningsNGCheckoutHook extends AbstractMultiParentHook {

    private static final Logger LOGGER = Logger.getLogger(WarningsNGCheckoutHook.class.getName());

    @Override
    protected String getParentFolder() {
        return "warnings-ng-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        PomData data = context.getPomData();
        return "warnings-ng-parent".equals(data.artifactId) // localCheckoutDir
                || "warnings-ng".equals(data.artifactId); // checkout
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
