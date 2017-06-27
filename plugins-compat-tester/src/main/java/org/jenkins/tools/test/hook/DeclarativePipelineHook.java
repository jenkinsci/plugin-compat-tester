package org.jenkins.tools.test.hook;


import hudson.model.UpdateSite;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.jenkins.tools.test.SCMManagerFactory;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Workaround for the declarative pipeline plugins since they are
 * stored in a central repository.
 */
public class DeclarativePipelineHook extends AbstractMultiParentHook {

    @Override
    protected List<String> getBundledPlugins() {
        return Arrays.asList("pipeline-model-api", "pipeline-model-definition", "pipeline-model-extensions", "pipeline-model-json-shaded", "pipeline-stage-tags-metadata");
    }

    @Override
    protected String getParentFolder() {
        return "pipeline-model-definition";
    }

    @Override
    protected String getParentUrl() {
        return "scm:git:git://github.com/jenkinsci/pipeline-model-definition-plugin.git";
    }

    @Override
    protected String getParentProjectName() {
        return "pipeline-model-definition";
    }
}
