package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

public class AwsJavaSdkHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "aws-java-sdk-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        PomData data = context.getPomData();
        return ("org.jenkins-ci.plugins".equals(data.groupId)
                        && "aws-java-sdk".equals(data.artifactId)
                        && "hpi".equals(data.getPackaging()))
                || ("org.jenkins-ci.plugins.aws-java-sdk".equals(data.groupId)
                        && data.artifactId.startsWith("aws-java-sdk")
                        && "hpi".equals(data.getPackaging()));
    }
}
