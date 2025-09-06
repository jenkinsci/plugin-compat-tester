package org.jenkins.tools.test.model.plugin_metadata;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jenkins.tools.test.exception.GradleExecutionException;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.gradle.ExternalGradleRunner;
import org.jenkins.tools.test.maven.ExpressionEvaluator;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.util.BuildSystem;
import org.jenkins.tools.test.util.BuildSystemUtils;

public class LocalCheckoutPluginMetadataExtractor {

    private static final Logger LOGGER = Logger.getLogger(LocalCheckoutPluginMetadataExtractor.class.getName());
    private static final String GRADLE_POM_PATH = "build/publications/mavenJpi/pom-default.xml";

    @NonNull
    private final File localCheckoutDir;

    @NonNull
    private final PluginCompatTesterConfig config;

    @NonNull
    private final MavenRunner runner;

    @NonNull
    private final File pomFile;

    private final Boolean isGradlePom;

    public LocalCheckoutPluginMetadataExtractor(@NonNull PluginCompatTesterConfig config, @NonNull MavenRunner runner) {
        this.localCheckoutDir = getLocalCheckoutDir(config);
        this.config = config;
        this.runner = runner;
        this.pomFile = resolvePomFile(localCheckoutDir, config);
        this.isGradlePom = !pomFile.getName().equals("pom.xml");
    }

    private static File resolvePomFile(File baseDir, PluginCompatTesterConfig config) {
        File rootPom = new File(baseDir, "pom.xml");
        if (rootPom.exists()) {
            return rootPom;
        }

        File gradlePom = new File(baseDir, GRADLE_POM_PATH);
        if (gradlePom.exists()) {
            LOGGER.log(Level.INFO, "Using Gradle-generated POM: {0}", gradlePom.getPath());
            return gradlePom;
        }

        if (BuildSystemUtils.detectBuildSystem(baseDir) == BuildSystem.GRADLE) {
            try {
                new ExternalGradleRunner(config)
                        .run(Map.of(), baseDir, null, null, "generatePomFileForMavenJpiPublication");
                if (gradlePom.exists()) {
                    return gradlePom;
                }
            } catch (GradleExecutionException e) {
                throw new IllegalStateException("Failed to generate POM file for Gradle Project.", e);
            }
        }

        throw new IllegalStateException(
                "Could not find a Maven POM file. Please ensure either pom.xml exists at the root "
                        + "or, for Gradle plugins, that '" + GRADLE_POM_PATH + "' exists. "
                        + "If this is a Gradle project, run 'gradlew generatePomFileForMavenJpiPublication' first.");
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
        if (isGradlePom) {
            Plugin plugin = getPluginFromGradlePom();
            if (plugin == null) {
                throw new MetadataExtractionException(
                        "Could not extract plugin metadata from Gradle POM at:" + pomFile);
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
        } else {
            List<String> modules = new ArrayList<>();
            modules.add(null); // Root module
            modules.addAll(getModules());
            for (String module : modules) {
                Plugin plugin = getPlugin(module);
                if (plugin == null) {
                    continue;
                }
                if (config.getExcludePlugins() != null
                        && config.getExcludePlugins().contains(plugin.getPluginId())) {
                    LOGGER.log(Level.INFO, "Plugin {0} in excluded plugins; skipping", plugin.getPluginId());
                } else if (config.getIncludePlugins() != null
                        && !config.getIncludePlugins().isEmpty()
                        && !config.getIncludePlugins().contains(plugin.getPluginId())) {
                    LOGGER.log(Level.INFO, "Plugin {0} not in included plugins; skipping", plugin.getPluginId());
                } else {
                    plugins.add(plugin);
                }
            }
        }

        if (plugins.isEmpty()) {
            throw new MetadataExtractionException("Found no plugins in " + localCheckoutDir);
        }
        plugins.sort(Comparator.comparing(Plugin::getPluginId));
        return List.copyOf(plugins);
    }

    private Plugin getPluginFromGradlePom() throws MetadataExtractionException {
        try {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(pomFile);
            Element root = doc.getRootElement();
            String packaging = root.elementText("packaging");
            if (!"hpi".equals(packaging)) {
                LOGGER.warning("Gradle POM is not 'hpi', skipping.");
                return null;
            }
            String pluginId = root.elementText("artifactId");
            String version = root.elementText("version");
            return toPlugin(pluginId, version, localCheckoutDir, ":" + pluginId);
        } catch (Exception e) {
            throw new MetadataExtractionException("Failed to parse Gradle-generated pom.xml: " + pomFile, e);
        }
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
