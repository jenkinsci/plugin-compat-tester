package org.jenkins.tools.test.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataHooks;
import org.junit.jupiter.api.Test;

class WarUtilsTest {

    @Test
    void testPlugins() throws Exception {
        File megaWar =  new File("target", "megawar.war");
        List<PluginMetadata> pms = WarUtils.extractPluginMetadataFromWar(megaWar, PluginMetadataHooks.loadExtractors(Collections.emptyList()));
        assertThat(pms, hasSize(1));
        PluginMetadata pm = pms.get(0);

        assertThat(pm, allOf(
                hasProperty("pluginId", is("text-finder")),
                hasProperty("scmUrl", is("https://github.com/jenkinsci/text-finder-plugin.git")),
                hasProperty("modulePath", nullValue()), // not a multi module
                hasProperty("gitCommit", startsWith("text-finder-1.")),
                hasProperty("name", is("Text Finder")),
                hasProperty("version", startsWith("1."))
                ));
    }

    @Test
    void testJenkinsVersion() throws Exception {
        File megaWar =  new File("target", "megawar.war");
        String extractCoreVersionFromWar = WarUtils.extractCoreVersionFromWar(megaWar);
        assertThat(extractCoreVersionFromWar, startsWith("2."));
    }

}
