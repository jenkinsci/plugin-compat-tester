package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginCompatTesterHookBeforeCheckout.class)
public class WarningsNGCheckoutHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "warnings-ng-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        return "warnings-ng".equals(context.getPlugin().name);
    }

    @Override
    protected String getPluginFolderName(@NonNull BeforeCheckoutContext context) {
        return "plugin";
    }
}
