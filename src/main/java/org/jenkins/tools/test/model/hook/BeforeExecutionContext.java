package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.List;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;

public final class BeforeExecutionContext extends StageContext {

    @NonNull private final File cloneDirectory;

    @NonNull private final List<String> args;

    public BeforeExecutionContext(
            @NonNull PluginMetadata pluginMetadata,
            @NonNull String coreVersion,
            @NonNull PluginCompatTesterConfig config,
            @NonNull File cloneDirectory,
            @NonNull List<String> args) {
        super(Stage.EXECUTION, pluginMetadata, coreVersion, config);
        this.cloneDirectory = cloneDirectory;
        this.args = args;
    }

    @NonNull
    public File getCloneDirectory() {
        return cloneDirectory;
    }

    @NonNull
    public List<String> getArgs() {
        return args;
    }
}
