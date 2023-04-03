package org.jenkins.tools.test.hook;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;
import org.junit.jupiter.api.Test;

class JacocoHookTest {

    @Test
    void testCheckMethod() {
        final JacocoHook hook = new JacocoHook();

        PluginMetadata pm =
                new PluginMetadata.Builder()
                        .withGitCommit("ignored")
                        .withGitURL("ignored")
                        .withVersion("ignored")
                        .withPluginId("jacoco")
                        .build();

        BeforeExecutionContext context = new BeforeExecutionContext(pm, null, null, null, null);
        assertTrue(hook.check(context));

        pm = new PluginMetadata.Builder(pm).withPluginId("other-plugin").build();
        context = new BeforeExecutionContext(pm, null, null, null, null);
        assertFalse(hook.check(context));
    }

    @Test
    void testAction() {
        final JacocoHook hook = new JacocoHook();

        List<String> args =
                new ArrayList<>(
                        List.of("hpi:resolve-test-dependencies", "hpi:test-hpl", "surefire:test"));
        BeforeExecutionContext context = new BeforeExecutionContext(null, null, null, null, args);

        hook.action(context);
        // order is importat
        assertThat(
                args,
                contains(
                        "jacoco:prepare-agent",
                        "hpi:resolve-test-dependencies",
                        "hpi:test-hpl",
                        "surefire:test"));

        args = new ArrayList<>(List.of("other-plugin:other-goal", "surefire:test"));
        context = new BeforeExecutionContext(null, null, null, null, args);
        hook.action(context);
        assertThat(
                args, contains("other-plugin:other-goal", "jacoco:prepare-agent", "surefire:test"));

        args = new ArrayList<>(List.of("element1", "element2", "element3", "element4"));
        context = new BeforeExecutionContext(null, null, null, null, args);
        hook.action(context);
        assertThat(args, contains("element1", "element2", "element3", "element4"));
    }
}
