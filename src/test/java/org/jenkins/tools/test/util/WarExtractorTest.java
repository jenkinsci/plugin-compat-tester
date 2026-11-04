package org.jenkins.tools.test.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WarExtractorTest {

    @Test
    void testExtractCoreVersion() throws Exception {
        WarExtractor warExtractor =
                new WarExtractor(new File("target", "megawar.war"), new ServiceHelper(Set.of()), Set.of(), Set.of());
        String coreVersion = warExtractor.extractCoreVersion();
        assertThat(coreVersion, startsWith("2."));
    }

    @Test
    void testExtractPlugins() throws Exception {
        WarExtractor warExtractor =
                new WarExtractor(new File("target", "megawar.war"), new ServiceHelper(Set.of()), Set.of(), Set.of());
        List<Plugin> plugins = warExtractor.extractPlugins();
        assertThat(plugins, hasSize(1));
        Plugin plugin = plugins.get(0);
        assertThat(
                plugin,
                allOf(
                        hasProperty("pluginId", is("text-finder")),
                        hasProperty("gitUrl", is("https://github.com/jenkinsci/text-finder-plugin.git")),
                        hasProperty("module", is(":text-finder")), // regardless if it is a multi-module or not
                        hasProperty("tag", startsWith("text-finder-1.")),
                        hasProperty("name", is("Text Finder")),
                        hasProperty("version", startsWith("1."))));
    }

    @Test
    void testExtractPluginsWithNoMatches() throws Exception {
        WarExtractor warExtractor = new WarExtractor(
                new File("target", "megawar.war"), new ServiceHelper(Set.of()), Set.of("bogus-plugin-id"), Set.of());
        assertThrows(MetadataExtractionException.class, () -> warExtractor.extractPlugins());
    }

    @Test
    void pluginVersionsIgnoreTestSelection(@TempDir Path dir) throws Exception {
        File war = dir.resolve("plugins.war").toFile();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Short-Name", "configuration-as-code");
        manifest.getMainAttributes().putValue("Plugin-Version", "1849.v3a_d20568000a_ (private-build)");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream plugin = new JarOutputStream(bytes, manifest)) {}
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(war))) {
            jar.putNextEntry(new JarEntry("WEB-INF/plugins/configuration-as-code.hpi"));
            jar.write(bytes.toByteArray());
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("WEB-INF/detached-plugins/ignored.hpi"));
            jar.write(bytes.toByteArray());
            jar.closeEntry();
        }
        WarExtractor extractor =
                new WarExtractor(war, new ServiceHelper(Set.of()), Set.of("other"), Set.of("configuration-as-code"));
        assertThat(extractor.extractPluginVersions(), is(Map.of("configuration-as-code", "1849.v3a_d20568000a_")));
        assertThrows(MetadataExtractionException.class, extractor::extractPlugins);
    }

    @Test
    void noPluginVersionsInEmptyWar(@TempDir Path dir) throws Exception {
        File war = dir.resolve("empty.war").toFile();
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(war))) {}
        WarExtractor extractor = new WarExtractor(war, new ServiceHelper(Set.of()), Set.of(), Set.of());
        assertThat(extractor.extractPluginVersions(), is(Map.of()));
    }
}
