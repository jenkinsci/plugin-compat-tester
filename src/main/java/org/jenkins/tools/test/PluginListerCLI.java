package org.jenkins.tools.test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.model.hook.HookOrderComprator;
import org.jenkins.tools.test.picocli.ExistingFileTypeConverter;
import org.jenkins.tools.test.plgugin_metadata.PluginMetadata;
import org.jenkins.tools.test.plgugin_metadata.PluginMetadataExtractor;
import org.jenkins.tools.test.util.ModelReader;
import picocli.CommandLine;

@CommandLine.Command(
        name = "list-plugins",
        mixinStandardHelpOptions = true,
        description =
                "List (non-detached) plugins and their associated repositories that the bundled in the war.",
        versionProvider = VersionProvider.class)
public class PluginListerCLI implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-w", "--war"},
            required = true,
            description = "Path to the WAR file to be examined.",
            converter = ExistingFileTypeConverter.class)
    private File warFile;

    @CommandLine.Option(
            names = {"-o", "--output"},
            required = false,
            description = "location of the file to write containing the plugin and reposiries.")
    private File output;

    @Override
    public Integer call() throws PluginCompatibilityTesterException {
        List<PluginMetadataExtractor> extractors =
                ServiceLoader.load(PluginMetadataExtractor.class).stream()
                        .map(e -> e.get())
                        .sorted(new HookOrderComprator())
                        .collect(Collectors.toList());

        try (JarFile war = new JarFile(warFile);
                Stream<JarEntry> entries = war.stream()) {
            /* obtain a list and then map.

            List<PluginMetadata> pml = entries.filter(PluginListerCLI::isInterestingPluginEntry).map(e -> getPluginDetails(extractors, war, e)).collect(Collectors.toList());

            Stream<Entry<String, List<PluginMetadata>>> map = pml.stream().map(PluginListerCLI::toMultiMapEntry);

            Map<String,List<PluginMetadata>> collect = map.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, PluginListerCLI::merge));
            */
            Map<String, List<PluginMetadata>> repoPluginList =
                    entries.filter(PluginListerCLI::isInterestingPluginEntry)
                            .map(e -> getPluginDetails(extractors, war, e))
                            .map(PluginListerCLI::toMultiMapEntry)
                            .collect(
                                    Collectors.toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue,
                                            PluginListerCLI::merge));
            if (output != null) {

            } else {
                for (Map.Entry<String, List<PluginMetadata>> entry : repoPluginList.entrySet()) {
                    System.out.println(formatEntry(entry));
                }
            }
        } catch (IOException e) {
            throw new PluginCompatibilityTesterException(
                    "Failed to obtain plugin information from " + warFile, e);
        } catch (WrappedPluginCompatibilityTesterException e) {
            throw e.unwarp();
        }
        return Integer.valueOf(3);
    }

    private static String formatEntry(Entry<String, List<PluginMetadata>> entry) {
        StringBuilder sb = new StringBuilder(entry.getKey());
        for (PluginMetadata pm : entry.getValue()) {
            sb.append("\t").append(pm.getPluginId());
        }
        return sb.toString();
    }

    /**
     * Check if the given JarEntry is an interesting plugin.
     *
     * @return {@code true} iff the je represents a plugin in {@code WEB-INF/plugins/}
     */
    private static boolean isInterestingPluginEntry(JarEntry je) {
        // ignore detached plugins
        return je.getName().startsWith("WEB-INF/plugins/") && je.getName().endsWith(".hpi");
    }

    /**
     * obtain the repository URL and plugin name from the given jar entry. The Given jar entry must
     * be a plugin otherwise the behaviour is undefined.
     *
     * @param je the {@link JarEntry} representing the plugin.
     * @return and Entry whose key is the SCM url and value is the plugin id.
     * @throws PluginCompatibilityTesterException
     */
    private PluginMetadata getPluginDetails(
            List<PluginMetadataExtractor> extractors, JarFile f, JarEntry je)
            throws WrappedPluginCompatibilityTesterException {
        // the entry is the HPI file

        Manifest manifest;
        Model model;
        String pluginId;
        try (JarInputStream jis = new JarInputStream(f.getInputStream(je))) {
            manifest = jis.getManifest();
            String groupId = manifest.getMainAttributes().getValue("Group-Id");
            String artifactId = pluginId = manifest.getMainAttributes().getValue("Short-Name");
            model = ModelReader.getModelFromHpi(groupId, artifactId, jis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (PluginCompatibilityTesterException e) {
            throw new WrappedPluginCompatibilityTesterException(e);
        }
        try {
            for (PluginMetadataExtractor e : extractors) {
                Optional<PluginMetadata> optionalMetadata =
                        e.extractMetadata(pluginId, manifest, model);
                if (optionalMetadata.isPresent()) {
                    return optionalMetadata.get();
                }
            }
        } catch (PluginSourcesUnavailableException e) {
            throw new WrappedPluginCompatibilityTesterException(e);
        }
        throw new WrappedPluginCompatibilityTesterException(
                new PluginSourcesUnavailableException(
                        "No metadata could be extracted for entry " + je.getName()));
    }

    /**
     * Convert the given {@code Map.Entry<String, String>} to a {@code Map.Entry<String,
     * List<String>}
     *
     * @param je the {@link JarEntry} representing the plugin.
     * @return and Entry whose key is the SCM url and value is the plugin id.
     */
    private static Map.Entry<String, List<PluginMetadata>> toMultiMapEntry(PluginMetadata pm) {
        List<PluginMetadata> list =
                new LinkedList<>(); // mutable list so we can merge without creating a new list
        // later.
        list.add(pm);
        return new AbstractMap.SimpleEntry<String, List<PluginMetadata>>(pm.getScmUrl(), list);
    }

    private static <T extends Collection<E>, E> T merge(T t1, T t2) {
        t1.addAll(t2);
        return t1;
    }

    private static final class WrappedPluginCompatibilityTesterException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        WrappedPluginCompatibilityTesterException(PluginCompatibilityTesterException wrappee) {
            super(wrappee);
        }

        WrappedPluginCompatibilityTesterException unwarp() {
            return (WrappedPluginCompatibilityTesterException) getCause();
        }
    }
}
