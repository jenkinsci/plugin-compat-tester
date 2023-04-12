package org.jenkins.tools.test.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataExtractor;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataHooks;

public class WarUtils {

    private static final Logger LOGGER = Logger.getLogger(WarUtils.class.getName());

    private static final String PREFIX = "WEB-INF/plugins/";

    private static final String SUFFIX = ".hpi";

    /**
     * Extract the Jenkins core version from the given WAR.
     *
     * @param war the Jenkins WAR file.
     * @return the Jenkins core version number
     */
    public static String extractCoreVersionFromWar(File war) throws MetadataExtractionException {
        try (JarFile jf = new JarFile(war)) {
            Manifest manifest = jf.getManifest();
            String value = manifest.getMainAttributes().getValue("Jenkins-Version");
            if (value == null) {
                throw new MetadataExtractionException("Jenkins WAR is missing required Manifest entry");
            }
            return value;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract Jenkins core version from " + war.toString(), e);
        }
    }

    public static List<PluginMetadata> extractPluginMetadataFromWar(
            File warFile,
            List<PluginMetadataExtractor> extractors,
            Set<String> includedPlugins,
            Set<String> excludedPlugins)
            throws MetadataExtractionException {
        List<PluginMetadata> result = new ArrayList<>();
        try (JarFile jf = new JarFile(warFile)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (isInteresting(entry, includedPlugins, excludedPlugins)) {
                    result.add(PluginMetadataHooks.getPluginDetails(extractors, jf, entry));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("I/O error occurred whilst extracting plugin metadata from WAR", e);
        }
        return result;
    }

    /**
     * Predicate that will check if the given {@link JarEntry} is an interesting plugin. Detached
     * plugins are ignored. If the plugin is excluded it will be ignored. if the set of included
     * plugins is not empty it will be ignored if it is not included
     *
     * @return {@code true} iff {@code entry} represents a plugin in {@code WEB-INF/plugins/}
     */
    private static boolean isInteresting(JarEntry entry, Set<String> includedPlugins, Set<String> excludedPlugins) {
        // Ignore detached plugins
        if (entry.getName().startsWith(PREFIX) && entry.getName().endsWith(SUFFIX)) {
            String pluginId =
                    entry.getName().substring(PREFIX.length(), entry.getName().length() - SUFFIX.length());
            if (excludedPlugins != null && excludedPlugins.contains(pluginId)) {
                LOGGER.log(Level.INFO, "Plugin {0} in excluded plugins; skipping", pluginId);
                return false;
            }
            if (includedPlugins != null && !includedPlugins.isEmpty() && !includedPlugins.contains(pluginId)) {
                LOGGER.log(Level.INFO, "Plugin {0} not in included plugins; skipping", pluginId);
                return false;
            }
            return true;
        }
        return false;
    }
}
