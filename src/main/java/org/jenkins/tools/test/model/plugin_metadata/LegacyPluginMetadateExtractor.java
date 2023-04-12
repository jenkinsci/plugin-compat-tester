package org.jenkins.tools.test.model.plugin_metadata;

import java.util.Optional;
import java.util.jar.Manifest;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.model.hook.HookOrder;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginMetadataExtractor.class)
@HookOrder(order = -1000)
public class LegacyPluginMetadateExtractor extends PluginMetadataExtractor {

    @Override
    public Optional<PluginMetadata> extractMetadata(String pluginId, Manifest manifest, Model model)
            throws MetadataExtractionException {
        // Any multi-module project must have been handled before now (either the modern hook or a
        // specific hook for a legacy multi-module project)
        if (model.getScm() == null) {
            return Optional.empty();
        }
        assert pluginId.equals(model.getArtifactId());
        return Optional.of(new PluginMetadata.Builder()
                .withPluginId(model.getArtifactId())
                .withName(model.getName())
                .withScmConnection(model.getScm().getConnection())
                // Not guaranteed to be a hash, but close enough for this legacy code path
                .withGitHash(model.getScm().getTag())
                .withTag(model.getScm().getTag())
                // Any multi-module projects have already been handled by now or require new hooks
                .withModule(null)
                .withVersion(model.getVersion())
                .build());
    }
}
