package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

/**
 * Workaround for the fact that Artifact Manager on S3 has a dependency (jclouds) which uses a newer version of
 * Guice than Jenkins core. In this case, we intentionally violate upper bounds checks to maintain compatibility with
 * core. This works because the version of Guice shipped by core (6.0.0) supports both javax and jakarta imports, while
 * the version of Guice required by jclouds (7.0.0) supports only jakarta imports. In other words, the compatibility
 * matrix of the older version is a superset of the compatibility matrix of the newer version.
 */
@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class ArtifactManagerS3Hook extends PluginCompatTesterHookBeforeExecution {
    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        return context.getPlugin().getPluginId().equals("artifact-manager-s3");
    }

    @Override
    public void action(@NonNull BeforeExecutionContext context) {
        context.getUpperBoundsExcludes().add("com.google.inject.extensions:guice-assistedinject");
        context.getUpperBoundsExcludes().add("com.google.inject:guice");
        context.getUpperBoundsExcludes().add("com.google.inject:guice:classes");
    }
}
