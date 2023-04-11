package org.jenkins.tools.test.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.model.Dependency;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.exception.WrappedPluginCompatibilityException;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataExtractor;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataHooks;

public class WarUtils {

    private static final Logger LOGGER = Logger.getLogger(WarUtils.class.getName());

    /**
     * Extract the Jenkins core version from the given war.
     *
     * @param war the jenkins war file.
     * @return a {@link Dependency} representing the jenkins-core artifact in the war.
     */
    public static String extractCoreVersionFromWar(File war) throws MetadataExtractionException {
        try (JarFile jf = new JarFile(war)) {
            Manifest manifest = jf.getManifest();
            String value = manifest.getMainAttributes().getValue("Jenkins-Version");
            if (value == null) {
                throw new MetadataExtractionException("Jenkins war is missing required Manifest entry");
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
            throws PluginCompatibilityTesterException {
        try (JarFile war = new JarFile(warFile);
                Stream<JarEntry> entries = war.stream()) {
            return entries.filter(new InterestingPluginFilter(includedPlugins, excludedPlugins))
                    .map(e -> PluginMetadataHooks.getPluginDetails(extractors, war, e))
                    .collect(Collectors.toList());
        } catch (WrappedPluginCompatibilityException e) {
            throw e.getCause();
        } catch (IOException e) {
            throw new UncheckedIOException("I/O error occurred whilst extracting plugin metadata from WAR", e);
        }
    }

    /**
     * Predicate that will check if the given {@link JarEntry} is an interesting plugin.
     * Detached plugins are ignored.
     * If the plugin is excluded it will be ignored.
     * if the set of included plugins is not empty it will be ignored if it is not included
     */
    private static class InterestingPluginFilter implements Predicate<JarEntry> {

        private final Set<String> include;
        private final Set<String> exclude;

        private InterestingPluginFilter(Set<String> includedPlugins, Set<String> excludedPlugins) {
            this.include = includedPlugins;
            this.exclude = excludedPlugins;
        }

        @Override
        /**
         * @return {@code true} iff {@code je} represents a plugin in {@code WEB-INF/plugins/}
         */
        public boolean test(JarEntry je) {
            // ignore detached plugins;
            if (je.getName().startsWith("WEB-INF/plugins/") && je.getName().endsWith(".hpi")) {
                String pluginName = je.getName().substring(16, je.getName().length() - 4);
                if (exclude != null && exclude.contains(pluginName)) {
                    LOGGER.log(Level.INFO, "Plugin {0} in excluded plugins; skipping", pluginName);
                    return false;
                }
                if (include != null && !include.isEmpty() && !include.contains(pluginName)) {
                    LOGGER.log(Level.INFO, "Plugin {0} not in included plugins; skipping", pluginName);
                    return false;
                }
                return true;
            }
            return false;
        }
    }
}
