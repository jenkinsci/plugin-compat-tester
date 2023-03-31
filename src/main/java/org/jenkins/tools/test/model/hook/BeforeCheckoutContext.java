package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;

public final class BeforeCheckoutContext extends StageContext {

    public BeforeCheckoutContext(
            @NonNull PluginMetadata pluginMetadata,
            @NonNull String coreVersion,
            @NonNull PluginCompatTesterConfig config) {
        super(Stage.CHECKOUT, pluginMetadata, coreVersion, config);
    }
}
