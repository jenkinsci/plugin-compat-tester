package org.jenkins.tools.test.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.List;
import java.util.Set;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;
import org.junit.jupiter.api.Test;

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
                        hasProperty("module", nullValue()), // not a multi-module project
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
}
