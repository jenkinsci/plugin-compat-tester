package org.jenkins.tools.test;

import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.model.PluginRemoting;
import org.jenkins.tools.test.model.PomData;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ScmConnectionSpecialCasesTest {

    private static void runComputeScmConnectionAgainst(String scmUrlToTest, String artifactId, String expectedComputedScmUrl) {
        PomData pom = new PomData(artifactId, scmUrlToTest);
        PluginRemoting.computeScmConnection(pom);
        assertThat(pom.getConnectionUrl(), is(equalTo(expectedComputedScmUrl)));
    }

    @Test
    public void shouldOldJavaNetSubversionRepoUrlBeenMigrated() throws Throwable{
        runComputeScmConnectionAgainst(
                "scm:svn:https://guest@svn.dev.java.net/svn/hudson/tags/scm-sync-configuration/scm-sync-configuration-0.0.1", // old java.net url
                "",
                "scm:svn:https://guest@svn.java.net/svn/hudson~svn/tags/scm-sync-configuration/scm-sync-configuration-0.0.1"
        );
        runComputeScmConnectionAgainst(
                "scm:svn:https://guest@hudson.dev.java.net/svn/hudson/tags/labeled-test-groups-publisher-1.2.6", // old java.net url
                "",
                "scm:svn:https://guest@svn.java.net/svn/hudson~svn/tags/labeled-test-groups-publisher-1.2.6"
        );
    }

    @Test
    public void shouldProjectArtifactIdCorrectlyReplacedInUrls() throws Throwable{
        runComputeScmConnectionAgainst(
                "scm:git:git://github.com/jenkinsci/${project.artifactId}.git", // ${project.artifactId}
                "scm-sync-configuration-plugin",
                "scm:git:git://github.com/jenkinsci/scm-sync-configuration-plugin.git"
        );
    }

    @Test
    public void shouldGithubUsernamedUrlBeFiltered() throws Throwable{
        runComputeScmConnectionAgainst(
                "scm:git:https://sikakura@github.com/jenkinsci/mail-commander-plugin.git", // user specific authent
                "",
                "scm:git:git://github.com/jenkinsci/mail-commander-plugin.git"
        );
    }

    @Test
    public void shouldPluginSuffixOnlyAppliedOnScmSyncConfiguration() throws Throwable{
        runComputeScmConnectionAgainst(
                "scm:git:git://github.com/jenkinsci/scm-sync-configuration.git",  // special case of scm-sync-configuration
                "scm-sync-configuration",
                "scm:git:git://github.com/jenkinsci/scm-sync-configuration-plugin.git"
        );
        runComputeScmConnectionAgainst(
                "scm:git:git://github.com/magnayn/Jenkins-AdaptivePlugin.git",
                "Jenkins-AdaptivePlugin",
                "scm:git:git://github.com/magnayn/Jenkins-AdaptivePlugin.git"
        );
        runComputeScmConnectionAgainst(
                "scm:git:git://github.com/jenkinsci/copy-project-link.git",
                "copy-project-link",
                "scm:git:git://github.com/jenkinsci/copy-project-link.git"
        );
    }

    @Test
    public void shouldGithubDotComBeFollowedBySlashes() throws Throwable{
        runComputeScmConnectionAgainst(
                "scm:git:git://github.com:cittools/artifactdeployer-plugin.git", // No / after github.com
                "",
                "scm:git:git://github.com/cittools/artifactdeployer-plugin.git"
        );
    }

    @Test
    public void shouldGithubBeAccessedWithGitProtocol() throws Throwable{
        runComputeScmConnectionAgainst(
                "scm:git:ssh://github.com/jenkinsci/artifactory-plugin.git", // ssh protocol requiring ssh host key
                "",
                "scm:git:git://github.com/jenkinsci/artifactory-plugin.git"
        );
        runComputeScmConnectionAgainst(
                "scm:git:http://github.com/jenkinsci/artifactory-plugin.git", // ssh protocol requiring ssh host key
                "",
                "scm:git:git://github.com/jenkinsci/artifactory-plugin.git"
        );
        runComputeScmConnectionAgainst(
                "scm:git:https://github.com/jenkinsci/cifs-plugin.git", // ssh protocol requiring ssh host key
                "",
                "scm:git:git://github.com/jenkinsci/cifs-plugin.git"
        );
    }

    @Test
    public void shouldGitHudsonRepoBeMigratedToJenkinsCI() throws Throwable{
        runComputeScmConnectionAgainst(
                "scm:git:git://github.com/hudson/hudson-clearcase-plugin.git", // hudson repository
                "",
                "scm:git:git://github.com/jenkinsci/hudson-clearcase-plugin.git"
        );
    }


    @Test
    public void shouldScmConnectionBeTrimed() throws Throwable{
        runComputeScmConnectionAgainst(
                "\n   scm:git:https://github.com/jenkinsci/cifs-plugin.git  \n   ", // ssh protocol requiring ssh host key
                "",
                "scm:git:git://github.com/jenkinsci/cifs-plugin.git"
        );
    }

    @Test
    public void shouldGitAtGithubReplaceByGitProtocol() throws Throwable {
        runComputeScmConnectionAgainst(
                "scm:git:git@github.com:jenkinsci/jquery-plugin.git", // git@github.com
                "",
                "scm:git:git://github.com/jenkinsci/jquery-plugin.git"
        );
    }

    @Test()
    public void shouldEmptyConnectionUrlImpliesGithubUrlGeneration() throws Throwable{
        runComputeScmConnectionAgainst(
                "", // ssh protocol requiring ssh host key
                "hudsontrayapp",
                "scm:git:git://github.com/jenkinsci/hudsontrayapp-plugin.git"  // Special case
        );
        runComputeScmConnectionAgainst(
                "", // ssh protocol requiring ssh host key
                "jenkinswalldisplay",
                "scm:git:git://github.com/jenkinsci/walldisplay-plugin.git"   // Special case
        );
    }
}
