package org.jenkins.tools.test.hook;

import java.util.Map;
import org.jenkins.tools.test.model.PomData;

public class AwsJavaSdkHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "aws-java-sdk-plugin";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        return ("org.jenkins-ci.plugins".equals(data.groupId)
                        && "aws-java-sdk".equals(data.artifactId)
                        && "hpi".equals(data.getPackaging()))
                || ("org.jenkins-ci.plugins.aws-java-sdk".equals(data.groupId)
                        && data.artifactId.startsWith("aws-java-sdk")
                        && "hpi".equals(data.getPackaging()));
    }
}
