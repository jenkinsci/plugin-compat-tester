package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.UpdateSite;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

public abstract class StageContext {

    @NonNull private final Stage stage;
    @NonNull private final UpdateSite.Plugin plugin;
    @NonNull private final Model model;
    @NonNull private final Dependency coreCoordinates;
    @NonNull private final PluginCompatTesterConfig config;

    public StageContext(
            @NonNull Stage stage,
            @NonNull UpdateSite.Plugin plugin,
            @NonNull Model model,
            @NonNull Dependency coreCoordinates,
            @NonNull PluginCompatTesterConfig config) {
        this.stage = stage;
        this.plugin = plugin;
        this.model = model;
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
    public Model getModel() {
        return model;
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
