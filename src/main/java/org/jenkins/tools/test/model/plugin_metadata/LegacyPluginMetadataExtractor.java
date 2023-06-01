package org.jenkins.tools.test.model.plugin_metadata;

import java.util.jar.Manifest;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.model.hook.HookOrder;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginMetadataExtractor.class)
@HookOrder(order = -1000)
public class LegacyPluginMetadataExtractor implements PluginMetadataExtractor {

    @Override
    public boolean isApplicable(String pluginId, Manifest manifest, Model model) {
        /*
         * Any multi-module project must have been handled before now (either the modern hook or a specific hook for a
         * legacy multi-module project).
         */
        return model.getScm() != null;
    }

    @Override
    public Plugin extractMetadata(String pluginId, Manifest manifest, Model model) throws MetadataExtractionException {
        assert pluginId.equals(model.getArtifactId());
        return new Plugin.Builder()
                .withPluginId(model.getArtifactId())
                .withName(model.getName())
                .withScmConnection(model.getScm().getConnection())
                // Not guaranteed to be a hash, but close enough for this legacy code path
                .withGitHash(model.getScm().getTag())
                .withTag(model.getScm().getTag())
                // Any multi-module projects have already been handled by now or require new hooks
                .withModule(model.getArtifactId())
                .withVersion(model.getVersion())
                .build();
    }
}
