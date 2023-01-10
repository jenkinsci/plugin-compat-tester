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

public class WarningsNGExecutionHookTest {

    @Test
    public void testCheckMethod() {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();
        final MavenCoordinates parent = new MavenCoordinates("org.jenkins-ci.plugins", "plugin", "3.57");

        PomData pomData = new PomData("warnings-ng", "hpi", "it-does-not-matter", "whatever", parent, "org.jenkins-ci.plugins");
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
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();

        Map<String, Object> info = new HashMap<>();
        info.put("args", Lists.newArrayList(
                "--define=forkCount=1",
                "hpi:resolve-test-dependencies",
                "hpi:test-hpl",
                "surefire:test"));
        Map<String, Object> afterAction = hook.action(info);
        List<String> args = (List<String>) afterAction.get("args");
        assertThat(args.size(), is(5));
        assertTrue(args.contains("failsafe:integration-test"));
    }
    
    @Test
    public void testType() throws Exception {
        final WarningsNGExecutionHook hook = new WarningsNGExecutionHook();

        Map<String, Object> info = new HashMap<>();
        info.put("types", Lists.newArrayList("surefire"));
        Map<String, Object> afterAction = hook.action(info);
        List<String> types = (List<String>) afterAction.get("types");
        assertThat(types.size(), is(2));
        assertTrue(types.contains("failsafe"));
    }
    
    
}