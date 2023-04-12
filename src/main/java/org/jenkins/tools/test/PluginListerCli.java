package org.jenkins.tools.test;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataExtractor;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataHooks;
import org.jenkins.tools.test.picocli.ExistingFileTypeConverter;
import org.jenkins.tools.test.util.WarUtils;
import picocli.CommandLine;

@CommandLine.Command(
        name = "list-plugins",
        mixinStandardHelpOptions = true,
        description = "List (non-detached) plugins and their associated repositories that the bundled in the WAR.",
        versionProvider = VersionProvider.class)
public class PluginListerCli implements Callable<Integer> {

    private static final Logger LOGGER = Logger.getLogger(PluginListerCli.class.getName());

    @CommandLine.Option(
            names = {"-w", "--war"},
            required = true,
            description = "Path to the WAR file to be examined.",
            converter = ExistingFileTypeConverter.class)
    private File warFile;

    @CommandLine.Option(
            names = "--external-hooks-jars",
            split = ",",
            arity = "1",
            paramLabel = "jar",
            description = "Comma-separated list of paths to external hooks JARs.",
            converter = ExistingFileTypeConverter.class)
    private Set<File> externalHooksJars = Set.of();

    @CheckForNull
    @CommandLine.Option(
            names = {"-o", "--output"},
            required = false,
            description = "Location of the file to write containing the plugin and repositories. "
                    + "The format of the file is a line per repository consiting of the repository URL followed "
                    + "by a tab followed by a tab separated list of plugins in that repository.")
    private File output;

    @CheckForNull
    @CommandLine.Option(
            names = "--include-plugins",
            split = ",",
            arity = "1",
            paramLabel = "plugin",
            description =
                    "Comma-separated list of plugin artifact IDs to test. If not set, every plugin in the WAR will be listed.")
    private Set<String> includePlugins;

    @CheckForNull
    @CommandLine.Option(
            names = "--exclude-plugins",
            split = ",",
            arity = "1",
            paramLabel = "plugin",
            description =
                    "Comma-separated list of plugin artifact IDs to skip. If not set, only the plugins specified by --plugins will be listed (or all plugins otherwise).")
    private Set<String> excludePlugins;

    @Override
    public Integer call() throws PluginCompatibilityTesterException {
        List<PluginMetadataExtractor> metadataExtractors = PluginMetadataHooks.loadExtractors(externalHooksJars);

        List<PluginMetadata> pluginMetadataList =
                WarUtils.extractPluginMetadataFromWar(warFile, metadataExtractors, includePlugins, excludePlugins);

        if (pluginMetadataList.isEmpty()) {
            LOGGER.log(Level.WARNING, "Found no plugins in {0}", warFile);
            return Integer.valueOf(5);
        }

        if (output != null) {
            // Group the plugins into their actual repositories.
            Map<String, List<PluginMetadata>> metaDataByRepoMap =
                    pluginMetadataList.stream().collect(Collectors.groupingBy(PluginMetadata::getGitUrl));

            try (BufferedWriter writer = Files.newBufferedWriter(output.toPath())) {
                for (Map.Entry<String, List<PluginMetadata>> entry : metaDataByRepoMap.entrySet()) {
                    writer.write(formatFileEntry(entry));
                    writer.write("\n");
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            // First find the longest String so we can pad correctly
            int max_length = 0;
            for (PluginMetadata pm : pluginMetadataList) {
                max_length = Math.max(max_length, pm.getPluginId().length());
            }
            // Add a couple of spaced padding for the longest entry.
            max_length += 2;
            System.out.println(
                    String.format(Locale.ROOT, "%-" + max_length + "s%s", "Plugin ID", "Git Repository URL"));
            for (PluginMetadata pm : pluginMetadataList) {
                System.out.println(
                        String.format(Locale.ROOT, "%-" + max_length + "s%s", pm.getPluginId(), pm.getGitUrl()));
            }
        }
        return Integer.valueOf(0);
    }

    /**
     * Format a list of plugins and git URLs as s ginel line consiting of the repository URL, followed by a tab character,
     * followed by the plugins each separated by the tab character.
     * @param entry the list of {@link PluginMetadata} describing the plugins in the
     * @return A single line of text describing the repositiry URL and the discovered plugins within it.
     */
    private static String formatFileEntry(Entry<String, List<PluginMetadata>> entry) {
        StringBuilder sb = new StringBuilder(entry.getKey());
        for (PluginMetadata pm : entry.getValue()) {
            sb.append("\t").append(pm.getPluginId());
        }
        return sb.toString();
    }
}
