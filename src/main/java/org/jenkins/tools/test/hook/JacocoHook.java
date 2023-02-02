package org.jenkins.tools.test.hook;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

/**
 * Workaround for JaCoCo plugin since it needs execute the jacoco:prepare-agent goal before
 * execution.
 */
public class JacocoHook extends PluginCompatTesterHookBeforeExecution {

    @Override
    public boolean check(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        return "jacoco".equals(data.artifactId);
    }

    @Override
    public Map<String, Object> action(Map<String, Object> info) {
        List<String> args = (List<String>) info.get("args");

        if (args != null) {
            int index =
                    IntStream.range(0, args.size())
                            .filter(i -> args.get(i).startsWith("hpi:"))
                            .findFirst()
                            .orElse(-1);
            if (index == -1) {
                index =
                        IntStream.range(0, args.size())
                                .filter(i -> args.get(i).equals("surefire:test"))
                                .findFirst()
                                .orElse(-1);
            }
            if (index != -1) {
                args.add(index, "jacoco:prepare-agent");
            }
        }

        return info;
    }
}
