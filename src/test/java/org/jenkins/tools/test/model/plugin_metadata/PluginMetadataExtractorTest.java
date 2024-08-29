package org.jenkins.tools.test.model.plugin_metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;

class PluginMetadataExtractorTest {

    @Test
    void extractMetadata() throws Exception {
        try (InputStream resourceAsStream =
                PluginMetadataExtractorTest.class.getResourceAsStream("PluginMetadataExtractorTest/MANIFEST.MF")) {
            assertNotNull(resourceAsStream);
            Manifest manifest = new Manifest(resourceAsStream);
            Plugin plugin = PluginMetadataExtractor.extractMetadata("aws-java-sdk-ec2", manifest, null);
            assertThat(
                    plugin,
                    allOf(
                            hasProperty("pluginId", is("aws-java-sdk-ec2")),
                            hasProperty("gitUrl", is("https://github.com/jenkinsci/aws-java-sdk-plugin.git")),
                            hasProperty("module", is(":aws-java-sdk-ec2")),
                            hasProperty("gitHash", is("938ad577f750694635f3c0160ac2110db5d6eb98")),
                            hasProperty("name", is("Amazon Web Services SDK :: EC2")),
                            hasProperty("version", startsWith("1.12.406-373.v59d2b_d41281b_"))));
        }
    }
}
