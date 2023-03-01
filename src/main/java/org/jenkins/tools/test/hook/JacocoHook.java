package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

/**
 * Workaround for JaCoCo plugin since it needs execute the jacoco:prepare-agent goal before
 * execution.
 */
public class JacocoHook extends PluginCompatTesterHookBeforeExecution {

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        Model model = context.getModel();
        return "jacoco".equals(model.getArtifactId());
    }

    @Override
    public void action(@NonNull BeforeExecutionContext context) {
        List<String> args = context.getArgs();
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
}
