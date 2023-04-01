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

class JacocoHookTest {

    @Test
    void testCheckMethod() {
        final JacocoHook hook = new JacocoHook();

        Model model = new Model();
        model.setGroupId("org.jenkins-ci.plugins");
        model.setArtifactId("jacoco");
        model.setPackaging("hpi");
        BeforeExecutionContext context = new BeforeExecutionContext(null, null, model, null, null, null);
        assertTrue(hook.check(context));

        model.setArtifactId("other-plugin");
        context = new BeforeExecutionContext(null, null, model, null, null, null);
        assertFalse(hook.check(context));
    }

    @Test
    void testAction() {
        final JacocoHook hook = new JacocoHook();

        List<String> args = new ArrayList<>(List.of("hpi:resolve-test-dependencies", "hpi:test-hpl", "surefire:test"));
        BeforeExecutionContext context = new BeforeExecutionContext(null, null, null, null, args, null);
        hook.action(context);
        assertThat(args.size(), is(4));
        assertThat(args.get(0), is("jacoco:prepare-agent"));

        args = new ArrayList<>(List.of("other-plugin:other-goal", "surefire:test"));
        context = new BeforeExecutionContext(null, null, null, null, args, null);
        hook.action(context);
        assertThat(args.size(), is(3));
        assertThat(args.get(1), is("jacoco:prepare-agent"));

        args = new ArrayList<>(List.of("element1", "element2", "element3", "element4"));
        context = new BeforeExecutionContext(null, null, null, null, args, null);
        hook.action(context);
        assertThat(args.size(), is(4));
        assertFalse(args.contains("jacoco:prepare-agent"));
    }
}
