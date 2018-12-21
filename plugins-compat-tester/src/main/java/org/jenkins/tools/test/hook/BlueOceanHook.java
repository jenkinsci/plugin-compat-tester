package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.SCMManagerFactory;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import hudson.model.UpdateSite.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Workaround for the blueocean plugins since they are
 * stored in a central repository.
 * 
 */
public class BlueOceanHook extends AbstractMultiParentHook {

    private static final Logger LOGGER = Logger.getLogger(BlueOceanHook.class.getName());

    @Override
    protected String getParentFolder() {
        return "blueocean";
    }

    @Override
    protected String getParentUrl() {
        return "scm:git:git://github.com/jenkinsci/blueocean-plugin.git";
    }

    @Override
    protected String getParentProjectName() {
        return "blueocean-parent";
    }

    @Override
    public boolean check(Map<String, Object> info) throws Exception {
        return isBOPlugin(info);
    }

    public static boolean isBOPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isBOPlugin(data);
    }

    public static boolean isBOPlugin(PomData data) {
        if (data.parent != null) {
            return data.parent.artifactId.equalsIgnoreCase("blueocean-parent");
        } else {
            LOGGER.log(Level.WARNING, "Artifact {0} has no parent POM, likely it was incrementalified (JEP-305). " +
                    "Will guess the plugin by artifact ID. FTR JENKINS-55169", data.artifactId);
            return data.artifactId.contains("blueocean");
        }
    }

}