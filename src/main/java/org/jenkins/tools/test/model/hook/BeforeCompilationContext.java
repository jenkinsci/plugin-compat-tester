package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;

public final class BeforeCompilationContext extends StageContext {

    @CheckForNull
    private final File cloneDirectory;

    public BeforeCompilationContext(
            @NonNull String coreVersion,
            @NonNull Plugin plugin,
            @NonNull PluginCompatTesterConfig config,
            @NonNull File cloneDirectory) {
        super(Stage.COMPILATION, coreVersion, plugin, config);
        this.cloneDirectory = cloneDirectory;
    }

    @CheckForNull
    public File getCloneDirectory() {
        return cloneDirectory;
    }
}
