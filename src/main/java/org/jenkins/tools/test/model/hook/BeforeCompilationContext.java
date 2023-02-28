package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.UpdateSite;
import java.io.File;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PomData;

public final class BeforeCompilationContext extends StageContext {

    @CheckForNull private final File pluginDir;

    @CheckForNull private final String parentFolder;

    private boolean ranCompile;

    public BeforeCompilationContext(
            @NonNull UpdateSite.Plugin plugin,
            @NonNull PomData pomData,
            @NonNull MavenCoordinates coreCoordinates,
            @NonNull PluginCompatTesterConfig config,
            @CheckForNull File pluginDir,
            @CheckForNull String parentFolder) {
        super(Stage.COMPILATION, plugin, pomData, coreCoordinates, config);
        this.pluginDir = pluginDir;
        this.parentFolder = parentFolder;
    }

    @CheckForNull
    public File getPluginDir() {
        return pluginDir;
    }

    @CheckForNull
    public String getParentFolder() {
        return parentFolder;
    }

    public boolean ranCompile() {
        return ranCompile;
    }

    public void setRanCompile(boolean ranCompile) {
        this.ranCompile = ranCompile;
    }
}
