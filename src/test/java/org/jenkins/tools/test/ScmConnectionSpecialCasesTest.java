/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Erik Ramfelt, Koichi Fujikawa, Red Hat, Inc., Seiji Sogabe,
 * Stephen Connolly, Tom Huybrechts, Yahoo! Inc., Alan Harder, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkins.tools.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginRemoting;
import org.jenkins.tools.test.model.PomData;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests about SCM connection URL transformations
 *
 * @author Frederic Camblor
 */
public class ScmConnectionSpecialCasesTest {

    private static void runComputeScmConnectionAgainst(
            String scmUrlToTest, String artifactId, String expectedComputedScmUrl) {
        PomData pom =
                new PomData(
                        artifactId,
                        "hpi",
                        scmUrlToTest,
                        "jenkins-2.150.1",
                        new MavenCoordinates("org.jenkins-ci.main", "cli", "2.150.1"),
                        "org.jenkins-ci.main");
        PluginRemoting.computeScmConnection(pom);
        assertThat(pom.getConnectionUrl(), is(equalTo(expectedComputedScmUrl)));
    }

    @Test
    public void shouldOldJavaNetSubversionRepoUrlBeenMigrated() {
        runComputeScmConnectionAgainst(
                // old java.net url
                "scm:svn:https://guest@svn.dev.java.net/svn/hudson/tags/scm-sync-configuration/scm-sync-configuration-0.0.1",
                "",
                "scm:svn:https://guest@svn.java.net/svn/hudson~svn/tags/scm-sync-configuration/scm-sync-configuration-0.0.1");
        runComputeScmConnectionAgainst(
                // old java.net url
                "scm:svn:https://guest@hudson.dev.java.net/svn/hudson/tags/labeled-test-groups-publisher-1.2.6",
                "",
                "scm:svn:https://guest@svn.java.net/svn/hudson~svn/tags/labeled-test-groups-publisher-1.2.6");
    }

    @Test
    public void shouldProjectArtifactIdCorrectlyReplacedInUrls() {
        runComputeScmConnectionAgainst(
                // ${project.artifactId}
                "scm:git:git://github.com/jenkinsci/${project.artifactId}.git",
                "scm-sync-configuration-plugin",
                "scm:git:git://github.com/jenkinsci/scm-sync-configuration-plugin.git");
    }

    @Ignore("disabled this transformation")
    @Test
    public void shouldGithubUsernamedUrlBeFiltered() {
        runComputeScmConnectionAgainst(
                // user specific authentication
                "scm:git:https://sikakura@github.com/jenkinsci/mail-commander-plugin.git",
                "",
                "scm:git:git://github.com/jenkinsci/mail-commander-plugin.git");
    }

    @Test
    public void shouldPluginSuffixOnlyAppliedOnScmSyncConfiguration() {
        runComputeScmConnectionAgainst(
                // special case of scm-sync-configuration
                "scm:git:git://github.com/jenkinsci/scm-sync-configuration.git",
                "scm-sync-configuration",
                "scm:git:git://github.com/jenkinsci/scm-sync-configuration-plugin.git");
        runComputeScmConnectionAgainst(
                "scm:git:git://github.com/magnayn/Jenkins-AdaptivePlugin.git",
                "Jenkins-AdaptivePlugin",
                "scm:git:git://github.com/magnayn/Jenkins-AdaptivePlugin.git");
        runComputeScmConnectionAgainst(
                "scm:git:git://github.com/jenkinsci/copy-project-link.git",
                "copy-project-link",
                "scm:git:git://github.com/jenkinsci/copy-project-link.git");
    }

    @Test
    public void shouldGithubDotComBeFollowedBySlashes() {
        runComputeScmConnectionAgainst(
                // No / after github.com
                "scm:git:git://github.com:cittools/artifactdeployer-plugin.git",
                "",
                "scm:git:git://github.com/cittools/artifactdeployer-plugin.git");
    }

    @Test
    public void shouldGitHudsonRepoBeMigratedToJenkinsCI() {
        runComputeScmConnectionAgainst(
                // hudson repository
                "scm:git:git://github.com/hudson/hudson-clearcase-plugin.git",
                "",
                "scm:git:git://github.com/jenkinsci/hudson-clearcase-plugin.git");
    }

    @Test
    public void shouldScmConnectionBeTrimmed() {
        runComputeScmConnectionAgainst(
                // ssh protocol requiring ssh host key
                "\n   scm:git:https://github.com/jenkinsci/cifs-plugin.git  \n   ",
                "",
                "scm:git:https://github.com/jenkinsci/cifs-plugin.git");
    }

    @Ignore("disabled this transformation")
    @Test
    public void shouldGitAtGithubReplaceByGitProtocol() {
        runComputeScmConnectionAgainst(
                // git@github.com
                "scm:git:git@github.com:jenkinsci/jquery-plugin.git",
                "",
                "scm:git:git://github.com/jenkinsci/jquery-plugin.git");
    }

    @Test()
    public void shouldEmptyConnectionUrlImpliesGithubUrlGeneration() {
        runComputeScmConnectionAgainst(
                // ssh protocol requiring ssh host key
                "",
                "hudsontrayapp",
                // Special case
                "scm:git:git://github.com/jenkinsci/hudsontrayapp-plugin.git");
        runComputeScmConnectionAgainst(
                // ssh protocol requiring ssh host key
                "",
                "jenkinswalldisplay",
                // Special case
                "scm:git:git://github.com/jenkinsci/walldisplay-plugin.git");
    }
}
