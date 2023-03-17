package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginCompatTesterHookBeforeCheckout.class)
public class AwsJavaSdkHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "aws-java-sdk-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        return context.getPlugin().name.startsWith("aws-java-sdk");
    }
}
