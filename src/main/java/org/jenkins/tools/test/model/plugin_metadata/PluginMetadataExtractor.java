package org.jenkins.tools.test.model.plugin_metadata;

import java.util.jar.Manifest;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.MetadataExtractionException;

/**
 * @author jnord
 */
public interface PluginMetadataExtractor {

    /**
     * Determine whether the extractor is applicable to the given plugin.
     */
    boolean isApplicable(String pluginId, Manifest manifest, Model model);

    /**
     * Obtain the metadata for a give plugin from a jar file.
     *
     * @param manifest the plugins' manifest.
     * @param model the plugins' model (from the HPI).
     * @return a fully populated {@link Plugin} for the given plugin.
     */
    Plugin extractMetadata(String pluginId, Manifest manifest, Model model) throws MetadataExtractionException;
}
