package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.UpdateSite;
import java.io.File;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PomData;

public final class BeforeCheckoutContext extends StageContext {

    private boolean ranCheckout;

    @CheckForNull private File checkoutDir;

    @CheckForNull private File pluginDir;

    @CheckForNull private String parentFolder;

    public BeforeCheckoutContext(
            @NonNull UpdateSite.Plugin plugin,
            @NonNull PomData pomData,
            @NonNull MavenCoordinates coreCoordinates,
            @NonNull PluginCompatTesterConfig config) {
        super(Stage.CHECKOUT, plugin, pomData, coreCoordinates, config);
    }

    public boolean ranCheckout() {
        return ranCheckout;
    }

    public void setRanCheckout(boolean ranCheckout) {
        this.ranCheckout = ranCheckout;
    }

    @CheckForNull
    public File getCheckoutDir() {
        return checkoutDir;
    }

    public void setCheckoutDir(@CheckForNull File checkoutDir) {
        this.checkoutDir = checkoutDir;
    }

    @CheckForNull
    public File getPluginDir() {
        return pluginDir;
    }

    public void setPluginDir(@CheckForNull File pluginDir) {
        this.pluginDir = pluginDir;
    }

    @CheckForNull
    public String getParentFolder() {
        return parentFolder;
    }

    public void setParentFolder(@CheckForNull String parentFolder) {
        this.parentFolder = parentFolder;
    }
}
