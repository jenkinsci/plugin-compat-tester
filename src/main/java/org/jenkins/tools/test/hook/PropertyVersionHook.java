package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.VersionNumber;
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
     * The minimum version needed. The version will be dynamically updated to this version (but only
     * if necessary).
     */
    public abstract String getMinimumVersion();

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        MavenRunner runner = new ExternalMavenRunner(context.getConfig());
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(
                context.getCloneDirectory(), context.getPlugin().getModule(), runner);
        try {
            String version = expressionEvaluator.evaluateString(getProperty());
            return new VersionNumber(version).isOlderThan(new VersionNumber(getMinimumVersion()));
        } catch (PomExecutionException e) {
            return false;
        }
    }

    @Override
    public void action(@NonNull BeforeExecutionContext context) {
        context.getArgs().add(String.format("-D%s=%s", getProperty(), getMinimumVersion()));
    }
}
