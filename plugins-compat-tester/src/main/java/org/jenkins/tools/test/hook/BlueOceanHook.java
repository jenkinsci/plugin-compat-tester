package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.SCMManagerFactory;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import hudson.model.UpdateSite.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Workaround for the blueocean plugins since they are
 * stored in a central repository.
 * 
 */
public class BlueOceanHook extends AbstractMultiParentHook {

    public static final List<String> BO_PLUGINS = Arrays.asList("blueocean", "blueocean-commons", "blueocean-config", "blueocean-dashboard", "blueocean-events", "blueocean-git-pipeline", "blueocean-github-pipeline", "blueocean-i18n", "blueocean-jwt", "blueocean-personalization", "blueocean-pipeline-api-impl", "blueocean-rest", "blueocean-rest-impl", "blueocean-web");

    @Override
    protected List<String> getBundledPlugins() {
        return BO_PLUGINS;
    }

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
}