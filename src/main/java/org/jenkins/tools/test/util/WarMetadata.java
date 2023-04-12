package org.jenkins.tools.test.util;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
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

public class WarMetadata {

    private static final Logger LOGGER = Logger.getLogger(WarMetadata.class.getName());

    private static final String PREFIX = "WEB-INF/plugins/";

    private static final String SUFFIX = ".hpi";

    @NonNull
    private final File warFile;

    @NonNull
    private final List<PluginMetadataExtractor> extractors;

    @CheckForNull
    private final Set<String> includedPlugins;

    @CheckForNull
    private final Set<String> excludedPlugins;

    public WarMetadata(
            File warFile, Set<File> externalHooksJars, Set<String> includedPlugins, Set<String> excludedPlugins) {
        this.warFile = warFile;
        this.extractors = PluginMetadataHooks.loadExtractors(externalHooksJars);
        this.includedPlugins = includedPlugins;
        this.excludedPlugins = excludedPlugins;
    }

    /**
     * Extract the Jenkins core version from the given WAR.
     *
     * @return the Jenkins core version number
     */
    public String getCoreVersion() throws MetadataExtractionException {
        try (JarFile jf = new JarFile(warFile)) {
            Manifest manifest = jf.getManifest();
            String value = manifest.getMainAttributes().getValue("Jenkins-Version");
            if (value == null) {
                throw new MetadataExtractionException("Jenkins WAR is missing required Manifest entry");
            }
            return value;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract Jenkins core version from " + warFile.toString(), e);
        }
    }

    public List<PluginMetadata> getPluginMetadata() throws MetadataExtractionException {
        List<PluginMetadata> result = new ArrayList<>();
        try (JarFile jf = new JarFile(warFile)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (isInteresting(entry)) {
                    result.add(PluginMetadataHooks.getPluginDetails(extractors, jf, entry));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("I/O error occurred whilst extracting plugin metadata from WAR", e);
        }
        if (result.isEmpty()) {
            throw new MetadataExtractionException("Found no plugins in " + warFile);
        }
        result.sort(Comparator.comparing(PluginMetadata::getPluginId));
        return List.copyOf(result);
    }

    /**
     * Predicate that will check if the given {@link JarEntry} is an interesting plugin. Detached
     * plugins are ignored. If the plugin is excluded it will be ignored. If the set of included
     * plugins is not empty it will be ignored if it is not included.
     *
     * @return {@code true} iff {@code entry} represents a plugin in {@code WEB-INF/plugins/}
     */
    private boolean isInteresting(JarEntry entry) {
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
