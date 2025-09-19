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

    private enum PomInfo {
        MAVEN_POM,
        GRADLE_POM
    }

    private record PomFile(File file, PomInfo type) {}

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
        File pomFile = resolvePomFileOrGenerate().file;
        boolean isGradlePom = resolvePomFileOrGenerate().type == PomInfo.GRADLE_POM;

        List<Plugin> plugins = new ArrayList<>();
        if (isGradlePom) {
            Plugin plugin = getPluginFromGradlePom(pomFile);
            if (plugin == null) {
                throw new MetadataExtractionException(
                        "Could not extract plugin metadata from Gradle POM at:" + pomFile);
            }
            shouldAddPlugin(plugins, plugin);
        } else {
            List<String> modules = new ArrayList<>();
            modules.add(null); // Root module
            modules.addAll(getModules());
            for (String module : modules) {
                Plugin plugin = getPlugin(module);
                if (plugin == null) {
                    continue;
                }
                shouldAddPlugin(plugins, plugin);
            }
        }

        if (plugins.isEmpty()) {
            throw new MetadataExtractionException("Found no plugins in " + localCheckoutDir);
        }
        plugins.sort(Comparator.comparing(Plugin::getPluginId));
        return List.copyOf(plugins);
    }

    private void shouldAddPlugin(List<Plugin> plugins, Plugin plugin) {
        if (config.getExcludePlugins().contains(plugin.getPluginId())) {
            LOGGER.log(Level.INFO, "Plugin {0} in excluded plugins; skipping", plugin.getPluginId());
        } else if (!config.getIncludePlugins().isEmpty()
                && !config.getIncludePlugins().contains(plugin.getPluginId())) {
            LOGGER.log(Level.INFO, "Plugin {0} not in included plugins; skipping", plugin.getPluginId());
        } else {
            plugins.add(plugin);
        }
    }

    @NonNull
    private PomFile resolvePomFileOrGenerate() throws MetadataExtractionException {
        File rootPom = new File(localCheckoutDir, "pom.xml");
        if (rootPom.exists()) {
            return new PomFile(rootPom, PomInfo.MAVEN_POM);
        }

        File gradlePom = new File(localCheckoutDir, GRADLE_POM_PATH);

        if (BuildSystemUtils.detectBuildSystem(localCheckoutDir) == BuildSystem.GRADLE_BUILD_TOOL) {
            try {
                new ExternalGradleRunner(config)
                        .run(Map.of(), localCheckoutDir, null, null, "generatePomFileForMavenJpiPublication");
                if (gradlePom.exists()) {
                    return new PomFile(gradlePom, PomInfo.GRADLE_POM);
                }
            } catch (GradleExecutionException e) {
                throw new MetadataExtractionException("Failed to generate POM file for Gradle Project.", e);
            }
        }

        throw new MetadataExtractionException(
                "Could not find a Maven POM file. Please ensure either pom.xml exists at the root "
                        + "or, for Gradle plugins, that '" + GRADLE_POM_PATH + "' exists. "
                        + "If this is a Gradle project, run 'gradlew generatePomFileForMavenJpiPublication' first.");
    }

    private Plugin getPluginFromGradlePom(@NonNull File pomFile) throws MetadataExtractionException {
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
