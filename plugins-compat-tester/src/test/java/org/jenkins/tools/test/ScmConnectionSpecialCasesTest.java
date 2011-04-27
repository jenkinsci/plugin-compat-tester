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
                "git://github.com/jenkinsci/scm-sync-configuration.git",
                "",
                "git://github.com/jenkinsci/scm-sync-configuration-plugin.git"
        );
    }

    @Test
    public void shouldOldJavaNetSubversionRepoUrlBeenMigrated(){
        runComputeScmConnectionAgainst(
                "https://guest@svn.dev.java.net/svn/hudson/tags/scm-sync-configuration/scm-sync-configuration-0.0.1",
                "",
                "https://guest@svn.java.net/svn/hudson~svn/tags/scm-sync-configuration/scm-sync-configuration-0.0.1"
        );
    }

    @Test
    public void shouldProjectArtifactIdCorrectlyReplacedInUrls(){
        runComputeScmConnectionAgainst(
                "git://github.com/jenkinsci/${project.artifactId}.git",
                "scm-sync-configuration-plugin",
                "git://github.com/jenkinsci/scm-sync-configuration-plugin.git"
        );
    }

    @Test
    public void shouldGithubUsernamedUrlBeFiltered(){
        runComputeScmConnectionAgainst(
                "https://sikakura@github.com/jenkinsci/mail-commander-plugin.git",
                "",
                "git://github.com/jenkinsci/mail-commander-plugin.git"
        );
    }
}
