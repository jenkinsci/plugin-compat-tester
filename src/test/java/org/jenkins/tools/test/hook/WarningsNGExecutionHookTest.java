package org.jenkins.tools.test.hook;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.junit.jupiter.api.Test;
import hudson.model.UpdateSite;

class WarningsNGExecutionHookTest {

    @Test
    void testCheckMethod() {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();

        UpdateSite.Plugin plugin = new UpdateSite.Plugin("warnings-ng", null, null, null);

        BeforeExecutionContext context =
                new BeforeExecutionContext(plugin, null, null, null, null, null, List.of(), null);
        UpdateSite.Plugin plugin2 = context.getPlugin();
        assertTrue(hook.check(context));

        plugin = new UpdateSite.Plugin("other-plugin", null, null, null);
        context = new BeforeExecutionContext(plugin, null, null, null, null, null, List.of(), null);
        assertFalse(hook.check(context));
    }

    @Test
    void testAction() {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();

        List<String> args =
                new ArrayList<>(
                        List.of("hpi:resolve-test-dependencies", "hpi:test-hpl", "surefire:test"));
        BeforeExecutionContext context =
                new BeforeExecutionContext(null, null, null, null, null, null, args, null);
        hook.action(context);
        assertThat(args.size(), is(4));
        assertTrue(args.contains("failsafe:integration-test"));
    }
}
