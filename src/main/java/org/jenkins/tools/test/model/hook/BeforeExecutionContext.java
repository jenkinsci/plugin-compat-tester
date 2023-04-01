package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.MavenPom;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

public final class BeforeExecutionContext extends StageContext {

    @NonNull
    private final Model model;

    @CheckForNull
    private final File pluginDir;

    @NonNull
    private final List<String> args;

    @NonNull
    private final MavenPom pom;

    public BeforeExecutionContext(
            @NonNull Dependency coreCoordinates,
            @NonNull PluginCompatTesterConfig config,
            @NonNull Model model,
            @CheckForNull File pluginDir,
            @NonNull List<String> args,
            @NonNull MavenPom pom) {
        super(Stage.EXECUTION, coreCoordinates, config);
        this.model = model;
        this.pluginDir = pluginDir;
        this.args = args;
        this.pom = pom;
    }

    @NonNull
    public Model getModel() {
        return model;
    }

    @CheckForNull
    public File getPluginDir() {
        return pluginDir;
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
