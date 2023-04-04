package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;

public final class BeforeCompilationContext extends StageContext {

    @CheckForNull
    private final File cloneDirectory;

    public BeforeCompilationContext(
            @NonNull PluginMetadata pluginMetadata,
            @NonNull String coreVersion,
            @NonNull PluginCompatTesterConfig config,
            @NonNull File cloneDirectory) {
        super(Stage.COMPILATION, pluginMetadata, coreVersion, config);
        this.cloneDirectory = cloneDirectory;
    }

    @CheckForNull
    public File getCloneDir() {
        return cloneDirectory;
    }
}
