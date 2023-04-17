package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;

public final class BeforeCheckoutContext extends StageContext {

    public BeforeCheckoutContext(
            @NonNull String coreVersion, @NonNull Plugin plugin, @NonNull PluginCompatTesterConfig config) {
        super(Stage.CHECKOUT, coreVersion, plugin, config);
    }
}
