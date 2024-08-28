package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    @SuppressFBWarnings(
            value = "UNSAFE_HASH_EQUALS",
            justification = "We are not used Git SHA comparisons for security")
    @Override
    public void action(@NonNull BeforeCheckoutContext context) throws PluginSourcesUnavailableException {
        String gitHash = context.getPlugin().getGitHash();
        if (gitHash == null || gitHash.equals("HEAD")) {
            throw new PluginSourcesUnavailableException("Failed to check out plugin sources for "
                    + context.getPlugin().getPluginId());
        }
    }
}
