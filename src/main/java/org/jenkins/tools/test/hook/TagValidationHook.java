package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginCompatTesterHookBeforeCheckout.class)
public class TagValidationHook extends PluginCompatTesterHookBeforeCheckout {

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        return context.getConfig().getLocalCheckoutDir() == null;
    }

    @Override
    public void action(@NonNull BeforeCheckoutContext context)
            throws PluginSourcesUnavailableException {
        String tag = context.getPluginMetadata().getGitCommit();
        if (tag == null || "HEAD".equals(tag)) {
            throw new PluginSourcesUnavailableException(
                    "Failed to check out plugin sources for "
                            + context.getPluginMetadata().getPluginId());
        }
    }
}
