package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;

public abstract class StageContext {

    @NonNull private final Stage stage;
    @NonNull private final PluginMetadata pluginMetadata;
    @NonNull private final String coreVersion;
    @NonNull private final PluginCompatTesterConfig config;

    public StageContext(
            @NonNull Stage stage,
            @NonNull PluginMetadata pluginMetadata,
            @NonNull String coreVersion,
            @NonNull PluginCompatTesterConfig config) {
        this.stage = stage;
        this.pluginMetadata = pluginMetadata;
        this.coreVersion = coreVersion;
        this.config = config;
    }

    @NonNull
    public Stage getStage() {
        return stage;
    }

    @NonNull
    public PluginMetadata getPluginMetadata() {
        return pluginMetadata;
    }

    @NonNull
    public String getCoreVersion() {
        return coreVersion;
    }

    @NonNull
    public PluginCompatTesterConfig getConfig() {
        return config;
    }
}
