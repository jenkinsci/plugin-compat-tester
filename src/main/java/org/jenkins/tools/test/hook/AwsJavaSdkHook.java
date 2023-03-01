package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

public class AwsJavaSdkHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "aws-java-sdk-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        Model model = context.getModel();
        return ("org.jenkins-ci.plugins".equals(model.getGroupId())
                        && "aws-java-sdk".equals(model.getArtifactId())
                        && "hpi".equals(model.getPackaging()))
                || ("org.jenkins-ci.plugins.aws-java-sdk".equals(model.getGroupId())
                        && model.getArtifactId().startsWith("aws-java-sdk")
                        && "hpi".equals(model.getPackaging()));
    }
}
