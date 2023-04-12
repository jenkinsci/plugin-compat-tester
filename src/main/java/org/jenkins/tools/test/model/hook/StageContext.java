package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;

public abstract class StageContext {

    @NonNull
    private final Stage stage;

    @NonNull
    private final String coreVersion;

    @NonNull
    private final Plugin plugin;

    @NonNull
    private final PluginCompatTesterConfig config;

    public StageContext(
            @NonNull Stage stage,
            @NonNull String coreVersion,
            @NonNull Plugin plugin,
            @NonNull PluginCompatTesterConfig config) {
        this.stage = stage;
        this.plugin = plugin;
        this.coreVersion = coreVersion;
        this.config = config;
    }

    @NonNull
    public Stage getStage() {
        return stage;
    }

    @NonNull
    public String getCoreVersion() {
        return coreVersion;
    }

    @NonNull
    public Plugin getPlugin() {
        return plugin;
    }

    @NonNull
    public PluginCompatTesterConfig getConfig() {
        return config;
    }
}
