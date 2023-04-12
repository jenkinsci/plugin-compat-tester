package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.List;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;

public final class BeforeExecutionContext extends StageContext {

    @NonNull
    private final File cloneDirectory;

    @NonNull
    private final List<String> args;

    public BeforeExecutionContext(
            @NonNull String coreVersion,
            @NonNull Plugin plugin,
            @NonNull PluginCompatTesterConfig config,
            @NonNull File cloneDirectory,
            @NonNull List<String> args) {
        super(Stage.EXECUTION, coreVersion, plugin, config);
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
