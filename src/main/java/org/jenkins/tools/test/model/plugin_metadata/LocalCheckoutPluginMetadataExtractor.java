package org.jenkins.tools.test.model.plugin_metadata;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExpressionEvaluator;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

public class LocalCheckoutPluginMetadataExtractor {

    private static final Logger LOGGER = Logger.getLogger(LocalCheckoutPluginMetadataExtractor.class.getName());

    @NonNull
    private final File localCheckoutDir;

    @NonNull
    private final PluginCompatTesterConfig config;

    @NonNull
    private final MavenRunner runner;

    public LocalCheckoutPluginMetadataExtractor(@NonNull PluginCompatTesterConfig config, @NonNull MavenRunner runner) {
        this.localCheckoutDir = getLocalCheckoutDir(config);
        this.config = config;
        this.runner = runner;
    }

    @NonNull
    private static File getLocalCheckoutDir(@NonNull PluginCompatTesterConfig config) {
        File result = config.getLocalCheckoutDir();
        if (result == null) {
            throw new AssertionError("Could never happen, but needed to silence SpotBugs");
        }
        return result;
    }

    public List<Plugin> extractMetadata() throws MetadataExtractionException, PomExecutionException {
        List<Plugin> plugins = new ArrayList<>();
        List<String> modules = new ArrayList<>();
        modules.add(null); // Root module
        modules.addAll(getModules());
        for (String module : modules) {
            Plugin plugin = getPlugin(module);
            if (plugin == null) {
                continue;
            }
            if (config.getExcludePlugins() != null && config.getExcludePlugins().contains(plugin.getPluginId())) {
                LOGGER.log(Level.INFO, "Plugin {0} in excluded plugins; skipping", plugin.getPluginId());
            } else if (config.getIncludePlugins() != null
                    && !config.getIncludePlugins().isEmpty()
                    && !config.getIncludePlugins().contains(plugin.getPluginId())) {
                LOGGER.log(Level.INFO, "Plugin {0} not in included plugins; skipping", plugin.getPluginId());
            } else {
                plugins.add(plugin);
            }
        }
        if (plugins.isEmpty()) {
            throw new MetadataExtractionException("Found no plugins in " + localCheckoutDir);
        }
        plugins.sort(Comparator.comparing(Plugin::getPluginId));
        return List.copyOf(plugins);
    }

    private List<String> getModules() throws PomExecutionException {
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(localCheckoutDir, null, runner);
        return expressionEvaluator.evaluateList("project.modules");
    }

    @CheckForNull
    private Plugin getPlugin(String module) throws PomExecutionException {
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(localCheckoutDir, module, runner);
        String packaging = expressionEvaluator.evaluateString("project.packaging");
        if ("hpi".equals(packaging)) {
            String pluginId = expressionEvaluator.evaluateString("project.artifactId");
            String version = expressionEvaluator.evaluateString("project.version");
            return toPlugin(pluginId, version, localCheckoutDir, ":" + pluginId);
        }
        return null;
    }

    private static Plugin toPlugin(String pluginId, String version, File cloneDirectory, String module) {
        Plugin.Builder builder = new Plugin.Builder();
        builder.withPluginId(pluginId);
        builder.withVersion(version);
        builder.withGitUrl(cloneDirectory.toURI().toString());
        builder.withModule(module);
        return builder.build();
    }
}
