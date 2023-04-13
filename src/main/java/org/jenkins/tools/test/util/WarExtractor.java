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
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataExtractor;

public class WarExtractor {

    private static final Logger LOGGER = Logger.getLogger(WarExtractor.class.getName());

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

    public WarExtractor(
            File warFile, ServiceHelper serviceHelper, Set<String> includedPlugins, Set<String> excludedPlugins) {
        this.warFile = warFile;
        this.extractors = serviceHelper.loadServices(PluginMetadataExtractor.class);
        this.includedPlugins = includedPlugins;
        this.excludedPlugins = excludedPlugins;
    }

    /**
     * Extract the Jenkins core version from the given WAR.
     *
     * @return The Jenkins core version..
     */
    public String extractCoreVersion() throws MetadataExtractionException {
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

    /**
     * Extract the list of plugins to be tested from the given WAR.
     *
     * @return An unmodifiable list of plugins to be tested, sorted by plugin ID.
     */
    public List<Plugin> extractPlugins() throws MetadataExtractionException {
        List<Plugin> plugins = new ArrayList<>();
        try (JarFile jf = new JarFile(warFile)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (isInteresting(entry)) {
                    plugins.add(getPlugin(jf, entry));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("I/O error occurred whilst extracting plugin metadata from WAR", e);
        }
        if (plugins.isEmpty()) {
            throw new MetadataExtractionException("Found no plugins in " + warFile);
        }
        plugins.sort(Comparator.comparing(Plugin::getPluginId));
        return List.copyOf(plugins);
    }

    /**
     * Predicate that will check if the given {@link JarEntry} is an interesting plugin. Detached
     * plugins are ignored. If the plugin is excluded, it will be ignored. If the set of included
     * plugins is not empty, the plugin will be ignored if it is not in the set of included plugins.
     *
     * @return {@code true} iff {@code entry} represents a plugin in {@code WEB-INF/plugins/}
     */
    private boolean isInteresting(JarEntry entry) {
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

    /**
     * Obtain the plugin metadata from the given JAR entry.
     * The given JAR entry must be a plugin; otherwise, the behaviour is undefined.
     *
     * @param entry The {@link JarEntry} representing the plugin.
     * @return The plugin metadata.
     */
    private Plugin getPlugin(JarFile jf, JarEntry entry) throws MetadataExtractionException {
        // The entry is the HPI file
        Manifest manifest;
        Model model;
        String pluginId;
        try (JarInputStream jis = new JarInputStream(jf.getInputStream(entry))) {
            manifest = jis.getManifest();
            String groupId = manifest.getMainAttributes().getValue("Group-Id");
            String artifactId = pluginId = manifest.getMainAttributes().getValue("Short-Name");
            model = ModelReader.getPluginModelFromHpi(groupId, artifactId, jis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // Once all plugins have adopted https://github.com/jenkinsci/maven-hpi-plugin/pull/436 this can be simplified
        LOGGER.log(Level.INFO, "Extracting metadata for {0}", pluginId);
        for (PluginMetadataExtractor extractor : extractors) {
            if (extractor.isApplicable(pluginId, manifest, model)) {
                return extractor.extractMetadata(pluginId, manifest, model);
            }
        }
        throw new MetadataExtractionException("No metadata could be extracted for entry " + entry.getName());
    }

    /**
     * Group the plugins by repository.
     *
     * @return A map of repositories to plugins, sorted by the plugin Git URL.
     */
    public static NavigableMap<String, List<Plugin>> byRepository(List<Plugin> plugins) {
        return plugins.stream().collect(Collectors.groupingBy(Plugin::getGitUrl, TreeMap::new, Collectors.toList()));
    }
}
