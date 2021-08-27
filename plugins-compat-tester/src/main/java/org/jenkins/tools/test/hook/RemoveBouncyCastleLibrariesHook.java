package org.jenkins.tools.test.hook;

import java.util.Map;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jenkins.tools.test.model.MavenPom;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

public class RemoveBouncyCastleLibrariesHook  extends PluginCompatTesterHookBeforeExecution {


    @Override
    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        MavenPom pom = (MavenPom) moreInfo.get("pom");

        if (isBouncyCastleAPIPlugin(moreInfo)) {
            // remove the bouncycastle libraries, as they will be provided by the environment
            pom.removeDependency("org.bouncycastle", "bcpkix-jdk15on");
            pom.removeDependency("org.bouncycastle", "bcprov-jdk15on");
            pom.removeDependency("org.bouncycastle", "bcutil-jdk15on");
        } else {
            // remove bouncycastle plugin if present, then re-add the plugin with all libraries excluded
            pom.removeDependency("org.jenkins-ci.plugins", "bouncycastle-api");
            
            // add the bouncy castle plugin back in dependencyManagement (how do we obtain the version), with exclusions on org.bouncycastle:*
            // TODO where can we get this version from?
            pom.addToPom("dependencyManagement/dependencies", getBCExclusionElement("2.23"));
        }
        return moreInfo;
    }

    @Override
    public boolean check(Map<String, Object> info) {
        // TODO uncomment me for production
        // return Boolean.parseBoolean(System.getenv("REMOVE_BC_DEPS"));
        return true;
    }
    
    private boolean isBouncyCastleAPIPlugin(Map<String, Object> info) {
        return info.get("pluginName").equals("bouncycastle-api"); 
    }

    private Element getBCExclusionElement(String version) {
        DocumentFactory df = org.dom4j.DocumentFactory.getInstance();
        Element dependency = df.createElement("dependency", "http://maven.apache.org/POM/4.0.0");
        dependency.addElement("groupId").addText("org.jenkins-ci.plugins");
        dependency.addElement("artifactId").addText("bouncycastle-api");
        dependency.addElement("version").addText(version);
        Element exclusion = dependency.addElement("exclusions").addElement("exclusion");
        exclusion.addElement("groupId").addText("*");
        exclusion.addElement("artifactId").addText("*");
        dependency.detach();
        return dependency;
    }


}