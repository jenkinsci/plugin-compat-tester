package org.jenkins.tools.test.hook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;
import org.jenkins.tools.test.util.ServiceHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigurationAsCodeHookTest {
    private static final String HARNESS = "io.jenkins.configuration-as-code:test-harness";
    private final ConfigurationAsCodeHook hook = new ConfigurationAsCodeHook();

    private BeforeExecutionContext context(String plugin, Map<String, String> versions) {
        return new BeforeExecutionContext(
                "2.544",
                new Plugin.Builder()
                        .withPluginId(plugin)
                        .withVersion("1.0")
                        .withGitUrl("unused")
                        .withModule(":" + plugin)
                        .build(),
                new PluginCompatTesterConfig(new File("target.war"), new File("work")),
                null,
                new ArrayList<>(),
                versions,
                Map.of("jenkins.version", "2.544", "set.changelist", "true", "ignore.dirt", "true"));
    }

    @Test
    void onlyAppliesWhenJcascIsOverridden() {
        assertTrue(hook.check(context("other", Map.of("configuration-as-code", "1849.v3a_d20568000a_"))));
        assertFalse(hook.check(context("other", Map.of())));
        assertFalse(
                hook.check(context("configuration-as-code", Map.of("configuration-as-code", "1849.v3a_d20568000a_"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1700.v6f448841296e", "1849.v3a_d20568000a_", "9999-newer"})
    void alignsExistingHarness(String previousVersion) throws Exception {
        BeforeExecutionContext context = context("other", Map.of("configuration-as-code", "1849.v3a_d20568000a_"));
        context.getOverrideVersions().put("unrelated:dependency", "1.0");
        hook.alignHarness(context, (properties, directory, module, log, args) -> {
            assertEquals(":other", module);
            assertEquals("io.jenkins.configuration-as-code", properties.get("includeGroupIds"));
            assertEquals("test-harness", properties.get("includeArtifactIds"));
            try {
                Files.writeString(
                        Path.of(properties.get("outputFile")),
                        "The following files have been resolved:\n   " + HARNESS + ":jar:" + previousVersion
                                + ":test\n");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        assertEquals(
                Map.of(HARNESS, "1849.v3a_d20568000a_", "unrelated:dependency", "1.0"), context.getOverrideVersions());
    }

    @Test
    void usesConfiguredMavenProperties() throws Exception {
        BeforeExecutionContext context = context("other", Map.of("configuration-as-code", "1849.v3a_d20568000a_"));
        context.getConfig()
                .setMavenProperties(Map.of("harness.version", "1700.v6f448841296e", "jenkins.version", "2.479.3"));
        hook.alignHarness(context, (properties, directory, module, log, args) -> {
            assertEquals("1700.v6f448841296e", properties.get("harness.version"));
            assertEquals("2.544", properties.get("jenkins.version"));
            assertEquals("true", properties.get("set.changelist"));
            assertEquals("true", properties.get("ignore.dirt"));
        });
        assertFalse(context.getMavenProperties().containsKey("outputFile"));
        assertEquals("2.479.3", context.getConfig().getMavenProperties().get("jenkins.version"));
        context.getConfig().setMavenProperties(Map.of("later.hook", "value"));
        assertEquals("value", context.getMavenProperties().get("later.hook"));
    }

    @Test
    void doesNotAddMissingHarness() throws Exception {
        BeforeExecutionContext context = context("other", Map.of("configuration-as-code", "1849.v3a_d20568000a_"));
        context.getConfig().setMavenProperties(Map.of("overrideVersions", "unrelated:dependency:1.0"));
        hook.alignHarness(context, (properties, directory, module, log, args) -> {});
        assertTrue(context.getOverrideVersions().isEmpty());
        assertEquals("unrelated:dependency:1.0", context.getMavenProperties().get("overrideVersions"));
    }

    @Test
    void preservesExistingOverride() throws Exception {
        BeforeExecutionContext context = context("other", Map.of("configuration-as-code", "1849.v3a_d20568000a_"));
        context.getOverrideVersions().put(HARNESS, "explicit");
        hook.alignHarness(context, (properties, directory, module, log, args) -> {
            try {
                Files.writeString(Path.of(properties.get("outputFile")), HARNESS + ":jar:old:test");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        assertEquals("explicit", context.getOverrideVersions().get(HARNESS));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "unrelated:dependency:1.0",
                "unrelated:dependency:1.0,io.jenkins.configuration-as-code:test-harness:explicit",
                "unrelated:dependency:0.9, unrelated:dependency:1.0, io.jenkins.configuration-as-code:test-harness:explicit"
            })
    void preservesConfiguredOverrides(String overrides) throws Exception {
        BeforeExecutionContext context = context("other", Map.of("configuration-as-code", "1849.v3a_d20568000a_"));
        context.getConfig().setMavenProperties(Map.of("overrideVersions", overrides));
        hook.alignHarness(context, (properties, directory, module, log, args) -> {
            try {
                Files.writeString(Path.of(properties.get("outputFile")), HARNESS + ":jar:old:test");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        assertEquals("1.0", context.getOverrideVersions().get("unrelated:dependency"));
        assertEquals(
                overrides.contains(HARNESS) ? "explicit" : "1849.v3a_d20568000a_",
                context.getOverrideVersions().get(HARNESS));
        assertEquals(overrides, context.getConfig().getMavenProperties().get("overrideVersions"));
    }

    @Test
    void rejectsMalformedConfiguredOverride() {
        BeforeExecutionContext context = context("other", Map.of("configuration-as-code", "1849.v3a_d20568000a_"));
        context.getConfig().setMavenProperties(Map.of("overrideVersions", "missing-version"));
        assertThrows(
                PomExecutionException.class,
                () -> hook.alignHarness(context, (properties, directory, module, log, args) -> {
                    try {
                        Files.writeString(Path.of(properties.get("outputFile")), HARNESS + ":jar:old:test");
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }));
        assertTrue(context.getOverrideVersions().isEmpty());
    }

    @Test
    void propagatesResolutionFailure() {
        BeforeExecutionContext context = context("other", Map.of("configuration-as-code", "1849.v3a_d20568000a_"));
        assertThrows(
                PomExecutionException.class,
                () -> hook.alignHarness(context, (properties, directory, module, log, args) -> {
                    throw new PomExecutionException("resolution failed");
                }));
        assertTrue(context.getOverrideVersions().isEmpty());
    }

    @Test
    void registeredAsExecutionHook() {
        assertTrue(new ServiceHelper(Set.of())
                .loadServices(PluginCompatTesterHookBeforeExecution.class).stream()
                        .anyMatch(ConfigurationAsCodeHook.class::isInstance));
    }
}
