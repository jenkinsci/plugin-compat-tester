package org.jenkins.tools.test.model.plugin_metadata;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.MetadataExtractionException;

/**
 * Extractor that obtains all the information from the plugin's manifest file.
 * This requires the plugin to have been built with {@code maven-hpi-plugin} 3.42 or newer.
 *
 * @author jnord
 */
public final class PluginMetadataExtractor {

    private static final Attributes.Name PLUGIN_SCM_CONNECTION = new Attributes.Name("Plugin-ScmConnection");
    private static final Attributes.Name PLUGIN_SCM_TAG = new Attributes.Name("Plugin-ScmTag");
    private static final Attributes.Name PLUGIN_ID = new Attributes.Name("Short-Name");
    private static final Attributes.Name PLUGIN_NAME = new Attributes.Name("Long-Name");
    private static final Attributes.Name PLUGIN_VERSION = new Attributes.Name("Plugin-Version");
    private static final Attributes.Name IMPLEMENTATION_BUILD = new Attributes.Name("Implementation-Build");

    // Suppress default constructor for noninstantiability
    private PluginMetadataExtractor() {
        throw new AssertionError();
    }

    /**
     * Obtain the metadata for a give plugin from a jar file.
     *
     * @param manifest the plugins' manifest.
     * @param model the plugins' model (from the HPI).
     * @return a fully populated {@link Plugin} for the given plugin.
     */
    @NonNull
    public static Plugin extractMetadata(String pluginId, Manifest manifest, Model model)
            throws MetadataExtractionException {
        // All the information is stored in the plugin's manifest
        Attributes mainAttributes = manifest.getMainAttributes();

        assert pluginId.equals(mainAttributes.getValue(PLUGIN_ID));

        return new Plugin.Builder()
                .withPluginId(mainAttributes.getValue(PLUGIN_ID))
                .withName(mainAttributes.getValue(PLUGIN_NAME))
                .withScmConnection(mainAttributes.getValue(PLUGIN_SCM_CONNECTION))
                .withTag(mainAttributes.getValue(PLUGIN_SCM_TAG))
                .withGitHash(mainAttributes.getValue(IMPLEMENTATION_BUILD))
                .withModule(":" + mainAttributes.getValue(PLUGIN_ID))
                .withVersion(mainAttributes.getValue(PLUGIN_VERSION))
                .build();
    }
}
