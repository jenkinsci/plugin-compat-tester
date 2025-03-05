package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.VersionNumber;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExpressionEvaluator;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

/**
 * A generic class for ensuring that a property-versioned dependency is at a certain minimum
 * version, dynamically setting the property to a newer version (but only if necessary).
 */
public abstract class PropertyVersionHook extends PluginCompatTesterHookBeforeExecution {

    /** The Maven property specifying the version. */
    public abstract String getProperty();

    /**
     * The default minimum version needed, used if the user does not provide a specific minimum version.
     */
    public abstract String getDefaultMinimumVersion();

    /**
     * The minimum version needed, as provided by the user or falling back to the default value provided by {@link
     * #getDefaultMinimumVersion()}. The version will be dynamically updated to this version (but only if necessary).
     */
    public final String getMinimumVersion(@NonNull BeforeExecutionContext context) {
        return Optional.ofNullable(context.getConfig().getMavenProperties().get(getProperty()))
                .orElse(getDefaultMinimumVersion());
    }

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        MavenRunner runner = new ExternalMavenRunner(context.getConfig());
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(
                context.getCloneDirectory(), context.getPlugin().getModule(), runner);
        try {
            String version = expressionEvaluator.evaluateString(getProperty());
            return new VersionNumber(version).isOlderThan(new VersionNumber(getMinimumVersion(context)));
        } catch (PomExecutionException e) {
            return false;
        }
    }

    @Override
    public void action(@NonNull BeforeExecutionContext context) {
        Map<String, String> mavenProperties =
                new LinkedHashMap<>(context.getConfig().getMavenProperties());
        mavenProperties.put(getProperty(), getMinimumVersion(context));
        context.getConfig().setMavenProperties(mavenProperties);
    }
}
