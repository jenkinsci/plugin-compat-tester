package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

/** Keep the JCasC test harness aligned with the plugin used at runtime. */
@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class ConfigurationAsCodeHook extends PluginCompatTesterHookBeforeExecution {
    private static final String PLUGIN = "configuration-as-code";
    private static final String HARNESS = "io.jenkins.configuration-as-code:test-harness";

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        return context.getPluginVersions().containsKey(PLUGIN)
                && !PLUGIN.equals(context.getPlugin().getPluginId());
    }

    @Override
    public void action(@NonNull BeforeExecutionContext context) throws PomExecutionException {
        alignHarness(context, new ExternalMavenRunner(context.getConfig()));
    }

    void alignHarness(BeforeExecutionContext context, MavenRunner runner) throws PomExecutionException {
        try {
            Path dependencies = Files.createTempFile("pct-jcasc-dependencies", ".txt");
            try {
                Map<String, String> properties = context.getMavenProperties();
                properties.put("includeGroupIds", "io.jenkins.configuration-as-code");
                properties.put("includeArtifactIds", "test-harness");
                properties.put("outputFile", dependencies.toString());
                properties.put("appendOutput", "false");
                runner.run(
                        properties,
                        context.getCloneDirectory(),
                        context.getPlugin().getModule(),
                        null,
                        "dependency:list");
                if (Files.readString(dependencies, StandardCharsets.UTF_8).contains(HARNESS + ":")) {
                    context.getOverrideVersions()
                            .putIfAbsent(HARNESS, context.getPluginVersions().get(PLUGIN));
                }
            } finally {
                Files.deleteIfExists(dependencies);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
