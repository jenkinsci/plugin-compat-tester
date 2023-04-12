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

class WarningsNGExecutionHookTest {

    @Test
    void testCheckMethod() {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();

        PluginMetadata pm = new PluginMetadata.Builder()
                .withGitHash("ignored")
                .withGitUrl("ignored")
                .withVersion("ignored")
                .withPluginId("warnings-ng")
                .build();

        BeforeExecutionContext context = new BeforeExecutionContext(pm, null, null, null, null);
        assertTrue(hook.check(context));

        pm = new PluginMetadata.Builder(pm).withPluginId("other-plugin").build();
        context = new BeforeExecutionContext(pm, null, null, null, null);
        assertFalse(hook.check(context));
    }

    @Test
    void testAction() {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();

        List<String> args = new ArrayList<>(List.of("hpi:resolve-test-dependencies", "hpi:test-hpl", "surefire:test"));
        BeforeExecutionContext context = new BeforeExecutionContext(null, null, null, null, args);
        hook.action(context);
        // failsafe tests after surefire to match a general build.
        assertThat(
                args,
                contains(
                        "hpi:resolve-test-dependencies", "hpi:test-hpl", "surefire:test", "failsafe:integration-test"));
    }
}
