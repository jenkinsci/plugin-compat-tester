package org.jenkins.tools.test.model.plugin_metadata;

import java.util.Optional;
import java.util.jar.Manifest;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.MetadataExtractionException;

/**
 * @author jnord
 */
public abstract class PluginMetadataExtractor {

    /**
     * Obtain the metadata for a give plugin from a jar file.
     *
     * @param manifest the plugins' manifest.
     * @param model the plugins' model (from the HPI).
     * @return a fully populated PluginMetadata (or empty) for the given plugin.
     */
    public abstract Optional<PluginMetadata> extractMetadata(
            String pluginId, Manifest manifest, Model model)
            throws MetadataExtractionException;
}
