package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

public final class BeforeCompilationContext extends StageContext {

    @NonNull private final Model model;

    public BeforeCompilationContext(
            @NonNull Dependency coreCoordinates,
            @NonNull PluginCompatTesterConfig config,
            @NonNull Model model) {
        super(Stage.COMPILATION, coreCoordinates, config);
        this.model = model;
    }

    @NonNull
    public Model getModel() {
        return model;
    }
}
