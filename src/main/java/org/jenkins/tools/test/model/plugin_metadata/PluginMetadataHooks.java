package org.jenkins.tools.test.model.plugin_metadata;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.exception.WrappedPluginCompatibilityException;
import org.jenkins.tools.test.model.hook.HookOrderComparator;
import org.jenkins.tools.test.util.ModelReader;

public class PluginMetadataHooks {

    private static final Logger LOGGER = Logger.getLogger(PluginMetadataHooks.class.getName());

    public static List<PluginMetadataExtractor> loadExtractors(Set<File> externalJars) {
        ClassLoader cl = setupExternalClassLoaders(externalJars);
        List<PluginMetadataExtractor> extractors = ServiceLoader.load(PluginMetadataExtractor.class, cl).stream()
                .map(e -> e.get())
                .sorted(new HookOrderComparator())
                .collect(Collectors.toList());
        return extractors;
    }

    private static ClassLoader setupExternalClassLoaders(Set<File> externalJars) {
        ClassLoader base = PluginMetadataHooks.class.getClassLoader();
        if (externalJars.isEmpty()) {
            return base;
        }
        List<URL> urls = new ArrayList<>();
        for (File jar : externalJars) {
            try {
                urls.add(jar.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new UncheckedIOException(e);
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), base);
    }

    /**
     * obtain the repository URL and plugin name from the given jar entry. The Given jar entry must
     * be a plugin otherwise the behaviour is undefined.
     *
     * @param je the {@link JarEntry} representing the plugin.
     * @return and Entry whose key is the SCM url and value is the plugin id.
     * @throws WrappedPluginCompatibilityException if an
     */
    public static PluginMetadata getPluginDetails(List<PluginMetadataExtractor> extractors, JarFile f, JarEntry je)
            throws WrappedPluginCompatibilityException {
        // the entry is the HPI file

        Manifest manifest;
        Model model;
        String pluginId;
        try (JarInputStream jis = new JarInputStream(f.getInputStream(je))) {
            manifest = jis.getManifest();
            String groupId = manifest.getMainAttributes().getValue("Group-Id");
            String artifactId = pluginId = manifest.getMainAttributes().getValue("Short-Name");
            model = ModelReader.getPluginsModelFromHpi(groupId, artifactId, jis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (PluginCompatibilityTesterException e) {
            throw new WrappedPluginCompatibilityException(e);
        }
        try {
            // once all plugins have adopted https://github.com/jenkinsci/maven-hpi-plugin/pull/436 this can be
            // simpliied
            LOGGER.log(Level.INFO, "Extracting metadata about {0}", pluginId);
            for (PluginMetadataExtractor e : extractors) {
                Optional<PluginMetadata> optionalMetadata = e.extractMetadata(pluginId, manifest, model);
                if (optionalMetadata.isPresent()) {
                    return optionalMetadata.get();
                }
            }
        } catch (MetadataExtractionException e) {
            throw new WrappedPluginCompatibilityException(e);
        }
        throw new WrappedPluginCompatibilityException(
                new PluginSourcesUnavailableException("No metadata could be extracted for entry " + je.getName()));
    }
}
