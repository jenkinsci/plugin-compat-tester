package org.jenkins.tools.test.hook;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PomData;
import org.junit.Test;

public class JacocoHookTest {

    @Test
    public void testCheckMethod() {
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
    public void testAction() {
        final JacocoHook hook = new JacocoHook();

        Map<String, Object> info = new HashMap<>();
        info.put(
                "args",
                new ArrayList<>(
                        List.of("hpi:resolve-test-dependencies", "hpi:test-hpl", "surefire:test")));
        Map<String, Object> afterAction = hook.action(info);
        List<String> args = (List<String>) afterAction.get("args");
        assertThat(args.size(), is(4));
        assertThat(args.get(0), is("jacoco:prepare-agent"));

        info = new HashMap<>();
        info.put("args", new ArrayList<>(List.of("other-plugin:other-goal", "surefire:test")));
        afterAction = hook.action(info);
        args = (List<String>) afterAction.get("args");
        assertThat(args.size(), is(3));
        assertThat(args.get(1), is("jacoco:prepare-agent"));

        info = new HashMap<>();
        info.put("args", new ArrayList<>(List.of("element1", "element2", "element3", "element4")));
        afterAction = hook.action(info);
        args = (List<String>) afterAction.get("args");
        assertThat(args.size(), is(4));
        assertFalse(args.contains("jacoco:prepare-agent"));
    }
}
