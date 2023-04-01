package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.MavenPom;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.UpdateSite;

public final class BeforeExecutionContext extends StageContext {

    @CheckForNull
    private final File pluginDir;

    @CheckForNull
    private final String parentFolder;

    @NonNull
    private final List<String> args;

    @NonNull
    private final MavenPom pom;

    public BeforeExecutionContext(
            @NonNull UpdateSite.Plugin plugin,
            @NonNull Model model,
            @NonNull Dependency coreCoordinates,
            @NonNull PluginCompatTesterConfig config,
            @CheckForNull File pluginDir,
            @CheckForNull String parentFolder,
            @NonNull List<String> args,
            @NonNull MavenPom pom) {
        super(Stage.EXECUTION, plugin, model, coreCoordinates, config);
        this.pluginDir = pluginDir;
        this.parentFolder = parentFolder;
        this.args = args;
        this.pom = pom;
    }

    @CheckForNull
    public File getPluginDir() {
        return pluginDir;
    }

    @CheckForNull
    public String getParentFolder() {
        return parentFolder;
    }

    @NonNull
    public List<String> getArgs() {
        return args;
    }

    @NonNull
    public MavenPom getPom() {
        return pom;
    }
}
