package org.jenkins.tools.test.model.plugin_metadata;

import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.model.hook.HookOrder;
import org.kohsuke.MetaInfServices;

/**
 * Extractor that obtains all the information from the plugin's manifiest file. This requires the
 * plugin to have been built with a version of {@code maven-hpi-plugin} with
 * https://github.com/jenkinsci/maven-hpi-plugin/pull/436
 */
@MetaInfServices(PluginMetadataExtractor.class)
@HookOrder(order = 1000) // just incase it ever needs to be overridden
public class ModernPluginMetadataExtractor extends PluginMetadataExtractor {

    // https://github.com/jenkinsci/maven-hpi-plugin/pull/436
    private static final Attributes.Name PLUGIN_GIT_HASH = new Attributes.Name("Plugin-GitHash");
    private static final Attributes.Name PLUGIN_SCM_CONNECTION = new Attributes.Name("Plugin-ScmConnection");
    private static final Attributes.Name PLUGIN_SCM_TAG = new Attributes.Name("Plugin-ScmTag");
    private static final Attributes.Name PLUGIN_MODULE = new Attributes.Name("Plugin-Module");
    private static final Attributes.Name PLUGIN_ID = new Attributes.Name("Short-Name");
    private static final Attributes.Name PLUGIN_NAME = new Attributes.Name("Long-Name");
    private static final Attributes.Name PLUGIN_VERSION = new Attributes.Name("Plugin-Version");

    @Override
    public Optional<Plugin> extractMetadata(String pluginId, Manifest manifest, Model model)
            throws MetadataExtractionException {
        // All the information is stored in the plugin's manifest
        Attributes mainAttributes = manifest.getMainAttributes();

        assert pluginId.equals(mainAttributes.getValue(PLUGIN_ID));

        if (mainAttributes.containsKey(PLUGIN_GIT_HASH)) {
            // We are new enough to be a modern plugin

            return Optional.of(new Plugin.Builder()
                    .withPluginId(mainAttributes.getValue(PLUGIN_ID))
                    .withName(mainAttributes.getValue(PLUGIN_NAME))
                    .withScmConnection(mainAttributes.getValue(PLUGIN_SCM_CONNECTION))
                    .withTag(mainAttributes.getValue(PLUGIN_SCM_TAG))
                    .withGitHash(mainAttributes.getValue(PLUGIN_GIT_HASH))
                    .withModule(mainAttributes.getValue(PLUGIN_MODULE))
                    .withVersion(mainAttributes.getValue(PLUGIN_VERSION))
                    .build());
        }
        return Optional.empty();
    }
}
