
package org.jenkins.tools.test.model;

import com.google.common.collect.ImmutableMap;
import hudson.util.VersionNumber;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;

public class MavenPomTest {

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Issue("https://github.com/jenkinsci/bom/pull/301#issuecomment-694518923")
    @Test public void addDependenciesWithTestsClassifier() throws Exception {
        File prj = tmp.newFolder();
        File pom = new File(prj, "pom.xml");
        FileUtils.copyURLToFile(MavenPomTest.class.getResource("credentials-binding-pom-before.xml"), pom);
        Map<String,VersionNumber> toAdd = new HashMap<>(Collections.singletonMap("trilead-api", new VersionNumber("1.0.8")));
        Map<String,VersionNumber> toReplace = new HashMap<>(ImmutableMap.of("credentials", new VersionNumber("2.3.12"), "ssh-credentials", new VersionNumber("1.18.1"), "plain-credentials", new VersionNumber("1.7")));
        Map<String,VersionNumber> toAddTest = new HashMap<>();
        Map<String,VersionNumber> toReplaceTest = new HashMap<>(ImmutableMap.of("workflow-scm-step", new VersionNumber("2.11"), "workflow-durable-task-step", new VersionNumber("2.35"), "display-url-api", new VersionNumber("2.3.3"), "script-security", new VersionNumber("1.74"), "workflow-cps", new VersionNumber("2.83")));
        toReplaceTest.putAll(ImmutableMap.of("workflow-support", new VersionNumber("3.5"), "workflow-job", new VersionNumber("2.39"), "workflow-basic-steps", new VersionNumber("2.20")));
        VersionNumber coreDep = new VersionNumber("2.164.3");
        Map<String,String> pluginGroupIds = new HashMap<>(ImmutableMap.of("credentials-binding", "org.jenkins-ci.plugins", "pipeline-build-step", "org.jenkins-ci.plugins", "credentials", "org.jenkins-ci.plugins", "jdk-tool", "org.jenkins-ci.plugins", "snakeyaml-api", "io.jenkins.plugins"));
        pluginGroupIds.putAll(ImmutableMap.of("workflow-step-api", "org.jenkins-ci.plugins.workflow", "plain-credentials", "org.jenkins-ci.plugins", "trilead-api", "org.jenkins-ci.plugins", "command-launcher", "org.jenkins-ci.plugins", "jquery", "org.jenkins-ci.plugins"));
        pluginGroupIds.putAll(ImmutableMap.of("matrix-project", "org.jenkins-ci.plugins", "jquery-detached", "org.jenkins-ci.ui", "ace-editor", "org.jenkins-ci.ui", "git", "org.jenkins-ci.plugins", "workflow-durable-task-step", "org.jenkins-ci.plugins.workflow"));
        pluginGroupIds.putAll(ImmutableMap.of("git-client", "org.jenkins-ci.plugins", "ssh-credentials", "org.jenkins-ci.plugins", "variant", "org.jenkins-ci.plugins", "cloudbees-folder", "org.jenkins-ci.plugins", "scm-api", "org.jenkins-ci.plugins"));
        pluginGroupIds.putAll(ImmutableMap.of("pipeline-stage-step", "org.jenkins-ci.plugins", "junit-attachments", "org.jenkins-ci.plugins", "durable-task", "org.jenkins-ci.plugins", "workflow-job", "org.jenkins-ci.plugins.workflow", "workflow-basic-steps", "org.jenkins-ci.plugins.workflow"));
        pluginGroupIds.putAll(ImmutableMap.of("jsch", "org.jenkins-ci.plugins", "timestamper", "org.jenkins-ci.plugins", "junit", "org.jenkins-ci.plugins", "apache-httpcomponents-client-4-api", "org.jenkins-ci.plugins", "structs", "org.jenkins-ci.plugins"));
        pluginGroupIds.putAll(ImmutableMap.of("workflow-cps-global-lib", "org.jenkins-ci.plugins.workflow", "configuration-as-code", "io.jenkins", "workflow-cps", "org.jenkins-ci.plugins.workflow", "workflow-support", "org.jenkins-ci.plugins.workflow", "ssh-slaves", "org.jenkins-ci.plugins"));
        pluginGroupIds.putAll(ImmutableMap.of("htmlpublisher", "org.jenkins-ci.plugins", "mailer", "org.jenkins-ci.plugins", "ansicolor", "org.jenkins-ci.plugins", "jackson2-api", "org.jenkins-ci.plugins", "workflow-scm-step", "org.jenkins-ci.plugins.workflow"));
        pluginGroupIds.putAll(ImmutableMap.of("display-url-api", "org.jenkins-ci.plugins", "token-macro", "org.jenkins-ci.plugins", "script-security", "org.jenkins-ci.plugins", "pipeline-input-step", "org.jenkins-ci.plugins", "branch-api", "org.jenkins-ci.plugins"));
        pluginGroupIds.putAll(ImmutableMap.of("workflow-api", "org.jenkins-ci.plugins.workflow", "git-server", "org.jenkins-ci.plugins"));
        List<String> toConvert = Collections.emptyList();
        new MavenPom(prj).addDependencies(toAdd, toReplace, toAddTest, toReplaceTest, coreDep, pluginGroupIds, toConvert);
        assertEquals(IOUtils.toString(MavenPomTest.class.getResource("credentials-binding-pom-after.xml")).replaceAll("(?m) +$", ""), FileUtils.readFileToString(pom).replaceAll("(?m) +$", ""));
    }

}
