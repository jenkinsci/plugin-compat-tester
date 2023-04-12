package org.jenkins.tools.test.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.io.File;
import java.util.List;
import java.util.Set;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;
import org.junit.jupiter.api.Test;

class WarMetadataTest {

    @Test
    void testPlugins() throws Exception {
        WarMetadata warMetadata = new WarMetadata(new File("target", "megawar.war"), Set.of(), Set.of(), Set.of());
        List<PluginMetadata> pluginMetadata = warMetadata.getPluginMetadata();
        assertThat(pluginMetadata, hasSize(1));
        PluginMetadata pm = pluginMetadata.get(0);
        assertThat(
                pm,
                allOf(
                        hasProperty("pluginId", is("text-finder")),
                        hasProperty("gitUrl", is("https://github.com/jenkinsci/text-finder-plugin.git")),
                        hasProperty("module", nullValue()), // not a multi-module project
                        hasProperty("tag", startsWith("text-finder-1.")),
                        hasProperty("name", is("Text Finder")),
                        hasProperty("version", startsWith("1."))));
    }

    @Test
    void testJenkinsVersion() throws Exception {
        WarMetadata warMetadata = new WarMetadata(new File("target", "megawar.war"), Set.of(), Set.of(), Set.of());
        String coreVersion = warMetadata.getCoreVersion();
        assertThat(coreVersion, startsWith("2."));
    }
}
