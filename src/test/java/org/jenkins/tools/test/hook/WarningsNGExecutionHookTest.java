package org.jenkins.tools.test.hook;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.junit.jupiter.api.Test;

class WarningsNGExecutionHookTest {

    @Test
    void testCheckMethod() {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();
        final MavenCoordinates parent =
                new MavenCoordinates("org.jenkins-ci.plugins", "plugin", "3.57");

        PomData pomData =
                new PomData(
                        "warnings-ng",
                        "hpi",
                        "it-does-not-matter",
                        "whatever",
                        parent,
                        "org.jenkins-ci.plugins");
        BeforeExecutionContext context =
                new BeforeExecutionContext(null, pomData, null, null, null, null, List.of(), null);
        assertTrue(hook.check(context));

        pomData =
                new PomData(
                        "other-plugin",
                        "hpi",
                        "it-does-not-matter",
                        "whatever",
                        parent,
                        "org.jenkins-ci.plugins");
        context =
                new BeforeExecutionContext(null, pomData, null, null, null, null, List.of(), null);
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
