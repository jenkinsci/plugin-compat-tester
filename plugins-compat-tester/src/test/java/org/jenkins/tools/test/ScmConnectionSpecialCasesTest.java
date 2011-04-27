package org.jenkins.tools.test;

import org.jenkins.tools.test.model.PluginRemoting;
import org.jenkins.tools.test.model.PomData;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ScmConnectionSpecialCasesTest {

    private static void runComputeScmConnectionAgainst(String scmUrlToTest, String artifactId, String expectedComputedScmUrl){
        PomData pom = new PomData(artifactId, scmUrlToTest);
        PluginRemoting.computeScmConnection(pom);
        assertThat(pom.getConnectionUrl(), is(equalTo(expectedComputedScmUrl)));
    }

    @Test
    public void shouldGitUrlsShouldEndsWithPlugin(){
        runComputeScmConnectionAgainst(
                "git://github.com/jenkinsci/scm-sync-configuration.git",  // No "-plugin" suffix on a jenkinsci module
                "",
                "git://github.com/jenkinsci/scm-sync-configuration-plugin.git"
        );
    }

    @Test
    public void shouldOldJavaNetSubversionRepoUrlBeenMigrated(){
        runComputeScmConnectionAgainst(
                "https://guest@svn.dev.java.net/svn/hudson/tags/scm-sync-configuration/scm-sync-configuration-0.0.1", // old java.net url
                "",
                "https://guest@svn.java.net/svn/hudson~svn/tags/scm-sync-configuration/scm-sync-configuration-0.0.1"
        );
    }

    @Test
    public void shouldProjectArtifactIdCorrectlyReplacedInUrls(){
        runComputeScmConnectionAgainst(
                "git://github.com/jenkinsci/${project.artifactId}.git", // ${project.artifactId}
                "scm-sync-configuration-plugin",
                "git://github.com/jenkinsci/scm-sync-configuration-plugin.git"
        );
    }

    @Test
    public void shouldGithubUsernamedUrlBeFiltered(){
        runComputeScmConnectionAgainst(
                "https://sikakura@github.com/jenkinsci/mail-commander-plugin.git", // user specific authent
                "",
                "git://github.com/jenkinsci/mail-commander-plugin.git"
        );
    }

    @Test
    public void shouldNonJenkinsCIProjectNotEndsWithPluginSuffix(){
        runComputeScmConnectionAgainst(
                "git://github.com/magnayn/Jenkins-AdaptivePlugin.git", // Not jenkinsci project => no -plugin suffix !
                "",
                "git://github.com/magnayn/Jenkins-AdaptivePlugin.git"
        );
    }

    @Test
    public void shouldGithubDotComBeFollowedBySlashes(){
        runComputeScmConnectionAgainst(
                "git://github.com:cittools/artifactdeployer-plugin.git", // No / after github.com
                "",
                "git://github.com/cittools/artifactdeployer-plugin.git"
        );
    }

    @Test
    public void shouldGithubBeAccessedWithGitProtocol(){
        runComputeScmConnectionAgainst(
                "ssh://github.com/jenkinsci/artifactory-plugin.git", // ssh protocol requiring ssh host key
                "",
                "git://github.com/jenkinsci/artifactory-plugin.git"
        );
        runComputeScmConnectionAgainst(
                "http://github.com/jenkinsci/artifactory-plugin.git", // ssh protocol requiring ssh host key
                "",
                "git://github.com/jenkinsci/artifactory-plugin.git"
        );
        runComputeScmConnectionAgainst(
                "https://github.com/jenkinsci/cifs-plugin.git", // ssh protocol requiring ssh host key
                "",
                "git://github.com/jenkinsci/cifs-plugin.git"
        );
    }

    @Test
    public void shouldScmConnectionBeTrimed(){
        runComputeScmConnectionAgainst(
                "\n   https://github.com/jenkinsci/cifs-plugin.git  \n   ", // ssh protocol requiring ssh host key
                "",
                "git://github.com/jenkinsci/cifs-plugin.git"
        );
    }
}
