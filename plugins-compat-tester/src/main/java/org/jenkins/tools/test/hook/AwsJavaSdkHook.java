package org.jenkins.tools.test.hook;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins.tools.test.model.PomData;

public class AwsJavaSdkHook extends AbstractMultiParentHook {
    private static final Logger LOGGER = Logger.getLogger(AwsJavaSdkHook.class.getName());

    @Override
    protected String getParentFolder() {
        return "aws-java-sdk";
    }

    @Override
    protected String getParentProjectName() {
        return "aws-java-sdk-parent";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        return isAwsJavaSdkPlugin(info);
    }

    private boolean isAwsJavaSdkPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isAwsJavaSdkPlugin(data);
    }

    private boolean isAwsJavaSdkPlugin(PomData data) {
        if (data.parent != null) {
            return data.parent.artifactId.equalsIgnoreCase("aws-java-sdk-parent");
        } else {
            LOGGER.log(Level.WARNING, "Artifact {0} has no parent POM, likely it was incrementalified (JEP-305). " +
                    "Will guess the plugin by artifact ID. FTR JENKINS-55169", data.artifactId);
            return data.artifactId.contains("aws-java-sdk");
        }
    }
}
