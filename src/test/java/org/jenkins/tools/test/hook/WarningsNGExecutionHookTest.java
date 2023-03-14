package org.jenkins.tools.test.hook;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.junit.jupiter.api.Test;

class WarningsNGExecutionHookTest {

    @Test
    void testCheckMethod() {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();

        Model model = new Model();
        model.setGroupId("io.jenkins.plugins");
        model.setArtifactId("warnings-ng-parent");
        model.setPackaging("pom");

        BeforeExecutionContext context =
                new BeforeExecutionContext(null, null, model, null, List.of(), null);
        assertTrue(hook.check(context));

        model.setArtifactId("other-plugin");
        context = new BeforeExecutionContext(null, null, model, null, List.of(), null);
        assertFalse(hook.check(context));
    }

    @Test
    void testAction() {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();

        List<String> args =
                new ArrayList<>(
                        List.of("hpi:resolve-test-dependencies", "hpi:test-hpl", "surefire:test"));
        BeforeExecutionContext context =
                new BeforeExecutionContext(null, null, null, null, args, null);
        hook.action(context);
        assertThat(args.size(), is(4));
        assertTrue(args.contains("failsafe:integration-test"));
    }
}
