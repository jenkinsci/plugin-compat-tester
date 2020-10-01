package org.jenkins.tools.test.hook;

import com.google.common.collect.Lists;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PomData;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JacocoHookTest {

    @Test
    public void testCheckMethod() {
        final JacocoHook hook = new JacocoHook();
        final MavenCoordinates parent = new MavenCoordinates("org.jenkins-ci.plugins", "plugin", "3.57");

        PomData pomData = new PomData("jacoco", "hpi", "it-does-not-matter", "whatever", parent, "org.jenkins-ci.plugins");
        Map<String, Object> info = new HashMap<>();
        info.put("pomData", pomData);
        assertTrue(hook.check(info));

        pomData = new PomData("other-plugin", "hpi", "it-does-not-matter", "whatever", parent, "org.jenkins-ci.plugins");
        info = new HashMap<>();
        info.put("pomData", pomData);
        assertFalse(hook.check(info));
    }

    @Test
    public void testAction() throws Exception {
        final JacocoHook hook = new JacocoHook();

        Map<String, Object> info = new HashMap<>();
        info.put("args", Lists.newArrayList(
                "--define=forkCount=1",
                "hpi:resolve-test-dependencies",
                "hpi:test-hpl",
                "surefire:test"));
        Map<String, Object> afterAction = hook.action(info);
        List<String> args = (List<String>) afterAction.get("args");
        assertThat(args.size(), is(5));
        assertThat(args.get(1), is("jacoco:prepare-agent"));

        info = new HashMap<>();
        info.put("args", Lists.newArrayList(
                "--define=forkCount=1",
                "other-plugin:other-goal",
                "surefire:test"));
        afterAction = hook.action(info);
        args = (List<String>) afterAction.get("args");
        assertThat(args.size(), is(4));
        assertThat(args.get(2), is("jacoco:prepare-agent"));

        info = new HashMap<>();
        info.put("args", Lists.newArrayList(
                "element1",
                "element2",
                "element3",
                "element4"));
        afterAction = hook.action(info);
        args = (List<String>) afterAction.get("args");
        assertThat(args.size(), is(4));
        assertFalse(args.contains("jacoco:prepare-agent"));
    }
}
