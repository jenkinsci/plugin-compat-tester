package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.maven.model.Dependency;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

public abstract class StageContext {

    @NonNull
    private final Stage stage;

    @NonNull
    private final Dependency coreCoordinates;

    @NonNull
    private final PluginCompatTesterConfig config;

    public StageContext(
            @NonNull Stage stage, @NonNull Dependency coreCoordinates, @NonNull PluginCompatTesterConfig config) {
        this.stage = stage;
        this.coreCoordinates = coreCoordinates;
        this.config = config;
    }

    @NonNull
    public Stage getStage() {
        return stage;
    }

    @NonNull
    public Dependency getCoreCoordinates() {
        return coreCoordinates;
    }

    @NonNull
    public PluginCompatTesterConfig getConfig() {
        return config;
    }
}
