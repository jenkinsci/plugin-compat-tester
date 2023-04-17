package org.jenkins.tools.test.model.plugin_metadata;

import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.model.hook.HookOrder;
import org.kohsuke.MetaInfServices;

// TODO delete once all non standard multi module plugins are using
// https://github.com/jenkinsci/maven-hpi-plugin/pull/436
@MetaInfServices(PluginMetadataExtractor.class)
@HookOrder(order = -500)
public class LegacyMultiModulePluginMetadataExtractor implements PluginMetadataExtractor {

    private final Set<String> GROUP_IDS = Set.of("io.jenkins.blueocean", "io.jenkins.plugins.mina-sshd-api");

    private final Set<String> STANDARD_PLUGIN_IDS = Set.of(
            "declarative-pipeline-migration-assistant",
            "declarative-pipeline-migration-assistant-api",
            "pipeline-model-api",
            "pipeline-model-definition",
            "pipeline-model-extensions",
            "pipeline-stage-tags-metadata");

    private final Map<String, String> NONSTANDARD_PLUGIN_IDS = Map.of(
            "configuration-as-code", "plugin",
            "pipeline-rest-api", "rest-api",
            "pipeline-stage-view", "ui",
            "swarm", "plugin",
            "warnings-ng", "plugin",
            "workflow-cps", "plugin");

    @Override
    public boolean isApplicable(String pluginId, Manifest manifest, Model model) {
        if (model.getScm() == null) {
            return false;
        }
        String groupId = manifest.getMainAttributes().getValue("Group-Id");
        if (GROUP_IDS.contains(groupId) || STANDARD_PLUGIN_IDS.contains(pluginId)) {
            return true;
        } else {
            return NONSTANDARD_PLUGIN_IDS.containsKey(pluginId);
        }
    }

    @Override
    public Plugin extractMetadata(String pluginId, Manifest manifest, Model model) throws MetadataExtractionException {
        assert pluginId.equals(model.getArtifactId());
        String groupId = manifest.getMainAttributes().getValue("Group-Id");
        Plugin.Builder builder = new Plugin.Builder()
                .withPluginId(model.getArtifactId())
                .withName(model.getName())
                .withScmConnection(model.getScm().getConnection())
                .withTag(model.getScm().getTag())
                // Not guaranteed to be a hash, but close enough for this legacy code path
                .withGitHash(model.getScm().getTag())
                .withVersion(model.getVersion() == null ? model.getParent().getVersion() : model.getVersion());
        if (GROUP_IDS.contains(groupId) || STANDARD_PLUGIN_IDS.contains(pluginId)) {
            return builder.withModule(pluginId).build();
        } else if (NONSTANDARD_PLUGIN_IDS.containsKey(pluginId)) {
            return builder.withModule(NONSTANDARD_PLUGIN_IDS.get(pluginId)).build();
        } else {
            throw new MetadataExtractionException("No metadata could be extracted for " + pluginId);
        }
    }
}
