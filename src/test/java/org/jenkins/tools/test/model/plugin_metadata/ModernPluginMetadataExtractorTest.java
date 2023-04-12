package org.jenkins.tools.test.model.plugin_metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Optional;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;

class ModernPluginMetadataExtractorTest {

    @Test
    void extractModernMetadata() throws Exception {
        // from https://github.com/jenkinsci/aws-java-sdk-plugin/pull/956/checks?check_run_id=12250637623
        try (InputStream resourceAsStream = ModernPluginMetadataExtractorTest.class.getResourceAsStream(
                "ModernPluginMetadataExtractorTest/modern/MANIFEST.MF")) {

            assertNotNull(resourceAsStream);
            Manifest mf = new Manifest(resourceAsStream);

            ModernPluginMetadataExtractor modernPluginMetadataExtractor = new ModernPluginMetadataExtractor();

            Optional<PluginMetadata> optionalMetadata =
                    modernPluginMetadataExtractor.extractMetadata("aws-java-sdk-ec2", mf, null);

            assertTrue(optionalMetadata.isPresent(), "metadata should be extracted from a modern manifest");

            assertThat(
                    optionalMetadata.get(),
                    allOf(
                            hasProperty("pluginId", is("aws-java-sdk-ec2")),
                            hasProperty("gitUrl", is("https://github.com/jenkinsci/aws-java-sdk-plugin.git")),
                            hasProperty("module", is("aws-java-sdk-ec2")),
                            hasProperty("gitHash", is("938ad577f750694635f3c0160ac2110db5d6eb98")),
                            hasProperty("name", is("Amazon Web Services SDK :: EC2")),
                            hasProperty("version", startsWith("1.12.406-373.v59d2b_d41281b_"))));
        }
    }

    @Test
    void extractLegacyMetadata() throws Exception {
        // from https://updates.jenkins.io/download/plugins/text-finder/1.23/text-finder.hpi
        try (InputStream resourceAsStream = ModernPluginMetadataExtractorTest.class.getResourceAsStream(
                "ModernPluginMetadataExtractorTest/legacy/MANIFEST.MF")) {

            assertNotNull(resourceAsStream);
            Manifest mf = new Manifest(resourceAsStream);

            ModernPluginMetadataExtractor modernPluginMetadataExtractor = new ModernPluginMetadataExtractor();

            Optional<PluginMetadata> optionalMetadata =
                    modernPluginMetadataExtractor.extractMetadata("text-finder", mf, null);

            assertFalse(optionalMetadata.isPresent(), "metadata should not be extracted from a legacy manifest");
        }
    }
}
