package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import org.jenkins.tools.test.model.PomData;

import java.io.File;
import java.util.Map;

public class WarningsNGCheckoutHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "warnings-ng-plugin";
    }


    @Override
    protected String getParentProjectName() {
        return "warnings-ng";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        return isWarningsNG(info);
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin) {
        return "plugin";
    }

    private boolean isWarningsNG(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isWarningsNG(data);
    }

    private boolean isWarningsNG(PomData data) {
        return "warnings-ng-parent".equals(data.artifactId) // localCheckoutDir
                || "warnings-ng".equals(data.artifactId); // checkout
    }

    @Override
    protected void configureLocalCheckOut(UpdateSite.Plugin currentPlugin, File localCheckoutDir, Map<String, Object> moreInfo) {
        File pluginDir = new File(localCheckoutDir, getPluginFolderName(currentPlugin));
        if (!pluginDir.exists() && !pluginDir.isDirectory()) {
            throw new RuntimeException("Invalid localCheckoutDir for " + currentPlugin.getDisplayName());
        }

        // Checkout already happened, don't run through again
        moreInfo.put("runCheckout", false);
        firstRun = false;

        // Change the "download"" directory; after download, it's simply used for reference
        System.out.println("Child path for " + currentPlugin.getDisplayName() + " " + pluginDir);
        moreInfo.put("checkoutDir", pluginDir);
        moreInfo.put("pluginDir", pluginDir);
    }
}
