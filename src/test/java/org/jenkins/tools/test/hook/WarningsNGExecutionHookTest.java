package org.jenkins.tools.test.hook;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PomData;
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
        Map<String, Object> info = new HashMap<>();
        info.put("pomData", pomData);
        assertTrue(hook.check(info));

        pomData =
                new PomData(
                        "other-plugin",
                        "hpi",
                        "it-does-not-matter",
                        "whatever",
                        parent,
                        "org.jenkins-ci.plugins");
        info = new HashMap<>();
        info.put("pomData", pomData);
        assertFalse(hook.check(info));
    }

    @Test
    void testAction() {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();

        Map<String, Object> info = new HashMap<>();
        info.put(
                "args",
                new ArrayList<>(
                        List.of("hpi:resolve-test-dependencies", "hpi:test-hpl", "surefire:test")));
        Map<String, Object> afterAction = hook.action(info);
        List<String> args = (List<String>) afterAction.get("args");
        assertThat(args.size(), is(4));
        assertTrue(args.contains("failsafe:integration-test"));
    }
}
