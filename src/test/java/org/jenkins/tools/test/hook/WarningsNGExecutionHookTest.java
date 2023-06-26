package org.jenkins.tools.test.hook;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;
import org.junit.jupiter.api.Test;

class WarningsNGExecutionHookTest {

    @Test
    void testCheckMethod() {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();

        Plugin plugin = new Plugin.Builder()
                .withGitHash("ignored")
                .withGitUrl("ignored")
                .withVersion("ignored")
                .withPluginId("warnings-ng")
                .withModule(":warnings-ng")
                .build();

        BeforeExecutionContext context = new BeforeExecutionContext(null, plugin, null, null, null);
        assertTrue(hook.check(context));

        plugin = new Plugin.Builder(plugin).withPluginId("other-plugin").build();
        context = new BeforeExecutionContext(null, plugin, null, null, null);
        assertFalse(hook.check(context));
    }

    @Test
    void testAction() {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();

        List<String> args = new ArrayList<>(
                List.of("hpi:resolve-test-dependencies", "hpi:test-hpl", "hpi:test-runtime", "surefire:test"));
        BeforeExecutionContext context = new BeforeExecutionContext(null, null, null, null, args);
        hook.action(context);
        // failsafe tests after surefire to match a general build.
        assertThat(
                args,
                contains(
                        "hpi:resolve-test-dependencies",
                        "hpi:test-hpl",
                        "hpi:test-runtime",
                        "surefire:test",
                        "failsafe:integration-test"));
    }
}
