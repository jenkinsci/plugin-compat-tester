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
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;
import org.jenkins.tools.test.picocli.ExistingFileTypeConverter;
import org.jenkins.tools.test.util.ServiceHelper;
import org.jenkins.tools.test.util.WarExtractor;
import picocli.CommandLine;

@CommandLine.Command(
        name = "list-plugins",
        mixinStandardHelpOptions = true,
        description = "List (non-detached) plugins and their associated repositories that the bundled in the WAR.",
        versionProvider = VersionProvider.class)
public class PluginListerCli implements Callable<Integer> {

    private static final Pattern PATTERN = Pattern.compile("^https://github.com/(.+?)/(.+?)(\\.git)?$");

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
            description = "Location of the file to write containing the plugins grouped by repository."
                    + " The format of the file is a line per repository; each line consists of"
                    + " the owner of the repository, a slash character, the name of the"
                    + " repository, a tab character, and a comma-separated list of plugins in"
                    + " that repository.")
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
    public Integer call() throws MetadataExtractionException {
        ServiceHelper serviceHelper = new ServiceHelper(externalHooksJars);
        WarExtractor warExtractor = new WarExtractor(warFile, serviceHelper, includePlugins, excludePlugins);
        List<Plugin> plugins = warExtractor.extractPlugins();

        if (output != null) {
            NavigableMap<String, List<Plugin>> pluginsByRepository = WarExtractor.byRepository(plugins);

            try (BufferedWriter writer = Files.newBufferedWriter(output.toPath())) {
                for (Map.Entry<String, List<Plugin>> entry : pluginsByRepository.entrySet()) {
                    Matcher matcher = PATTERN.matcher(entry.getKey());
                    if (matcher.find()) {
                        writer.write(matcher.group(1));
                        writer.write('/');
                        writer.write(matcher.group(2));
                    } else {
                        throw new IllegalArgumentException("Invalid GitHub URL: " + entry.getKey());
                    }
                    writer.write('\t');
                    writer.write(
                            entry.getValue().stream().map(Plugin::getPluginId).collect(Collectors.joining(",")));
                    writer.newLine();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            // First find the longest String so we can pad correctly
            int maxLength = plugins.stream()
                    .map(Plugin::getPluginId)
                    .map(String::length)
                    .max(Integer::compareTo)
                    .get();

            // Add some padding for the longest entry
            maxLength += 4;

            System.out.println(String.format(Locale.ROOT, "%-" + maxLength + "s%s", "PLUGIN", "REPOSITORY"));
            for (Plugin plugin : plugins) {
                System.out.println(
                        String.format(Locale.ROOT, "%-" + maxLength + "s%s", plugin.getPluginId(), plugin.getGitUrl()));
            }
        }

        return Integer.valueOf(0);
    }
}
