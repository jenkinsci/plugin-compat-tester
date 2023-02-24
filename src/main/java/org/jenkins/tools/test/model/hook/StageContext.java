package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.UpdateSite;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PomData;

public abstract class StageContext {

    @NonNull private final Stage stage;
    @NonNull private final UpdateSite.Plugin plugin;
    @NonNull private final PomData pomData;
    @NonNull private final MavenCoordinates coreCoordinates;
    @NonNull private final PluginCompatTesterConfig config;

    public StageContext(
            @NonNull Stage stage,
            @NonNull UpdateSite.Plugin plugin,
            @NonNull PomData pomData,
            @NonNull MavenCoordinates coreCoordinates,
            @NonNull PluginCompatTesterConfig config) {
        this.stage = stage;
        this.plugin = plugin;
        this.pomData = pomData;
        this.coreCoordinates = coreCoordinates;
        this.config = config;
    }

    @NonNull
    public Stage getStage() {
        return stage;
    }

    @NonNull
    public UpdateSite.Plugin getPlugin() {
        return plugin;
    }

    @NonNull
    public PomData getPomData() {
        return pomData;
    }

    @NonNull
    public MavenCoordinates getCoreCoordinates() {
        return coreCoordinates;
    }

    @NonNull
    public PluginCompatTesterConfig getConfig() {
        return config;
    }
}
