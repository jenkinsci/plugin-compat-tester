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

class JacocoHookTest {

    @Test
    void testCheckMethod() {
        final JacocoHook hook = new JacocoHook();
        final MavenCoordinates parent =
                new MavenCoordinates("org.jenkins-ci.plugins", "plugin", "3.57");

        PomData pomData =
                new PomData(
                        "jacoco",
                        "hpi",
                        "it-does-not-matter",
                        "whatever",
                        parent,
                        "org.jenkins-ci.plugins");
        BeforeExecutionContext context =
                new BeforeExecutionContext(null, pomData, null, null, null, null, null, null);
        assertTrue(hook.check(context));

        pomData =
                new PomData(
                        "other-plugin",
                        "hpi",
                        "it-does-not-matter",
                        "whatever",
                        parent,
                        "org.jenkins-ci.plugins");
        context = new BeforeExecutionContext(null, pomData, null, null, null, null, null, null);
        assertFalse(hook.check(context));
    }

    @Test
    void testAction() {
        final JacocoHook hook = new JacocoHook();

        List<String> args =
                new ArrayList<>(
                        List.of("hpi:resolve-test-dependencies", "hpi:test-hpl", "surefire:test"));
        BeforeExecutionContext context =
                new BeforeExecutionContext(null, null, null, null, null, null, args, null);
        hook.action(context);
        assertThat(args.size(), is(4));
        assertThat(args.get(0), is("jacoco:prepare-agent"));

        args = new ArrayList<>(List.of("other-plugin:other-goal", "surefire:test"));
        context = new BeforeExecutionContext(null, null, null, null, null, null, args, null);
        hook.action(context);
        assertThat(args.size(), is(3));
        assertThat(args.get(1), is("jacoco:prepare-agent"));

        args = new ArrayList<>(List.of("element1", "element2", "element3", "element4"));
        context = new BeforeExecutionContext(null, null, null, null, null, null, args, null);
        hook.action(context);
        assertThat(args.size(), is(4));
        assertFalse(args.contains("jacoco:prepare-agent"));
    }
}
