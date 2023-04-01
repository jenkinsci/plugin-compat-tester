package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.maven.model.Dependency;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

public final class BeforeCheckoutContext extends StageContext {
    public BeforeCheckoutContext(@NonNull Dependency coreCoordinates, @NonNull PluginCompatTesterConfig config) {
        super(Stage.CHECKOUT, coreCoordinates, config);
    }
}
