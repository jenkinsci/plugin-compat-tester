package org.jenkins.tools.test.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.model.Dependency;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.exception.WrappedPluginCompatabilityException;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataExtractor;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataHooks;

public class WarUtils {

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
                throw new MetadataExtractionException(
                        "Jenkis war is missing required Manifest entry");
            }
            return value;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to extract Jenkins core version from " + war.toString(), e);
        }
    }

    public static List<PluginMetadata> extractPluginMetadataFromWar(
            File warFile, List<PluginMetadataExtractor> extractors)
            throws PluginCompatibilityTesterException {
        try (JarFile war = new JarFile(warFile);
                Stream<JarEntry> entries = war.stream()) {
            return entries.filter(WarUtils::isInterestingPluginEntry)
                    .map(e -> PluginMetadataHooks.getPluginDetails(extractors, war, e))
                    .collect(Collectors.toList());
        } catch (WrappedPluginCompatabilityException e) {
            throw e.getCause();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "I/O error occured whilst extracting plugin metadata from war", e);
        }
    }

    /**
     * Check if the given {@link JarEntry} is an interesting plugin. Detached plugins are ignored.
     *
     * @return {@code true} iff {@code je} represents a plugin in {@code WEB-INF/plugins/}
     */
    private static boolean isInterestingPluginEntry(JarEntry je) {
        // ignore detached plugins;
        return je.getName().startsWith("WEB-INF/plugins/") && je.getName().endsWith(".hpi");
    }
}
