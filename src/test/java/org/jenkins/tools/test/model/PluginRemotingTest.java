package org.jenkins.tools.test.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import org.junit.jupiter.api.Test;

class PluginRemotingTest {

    @Test
    void testStringInterpolation() {
        assertThat(
                PluginRemoting.interpolateString("${project.artifactId}", "wibble"), is("wibble"));
        assertThat(
                PluginRemoting.interpolateString("prefix-${project.artifactId}", "blah"),
                is("prefix-blah"));
        assertThat(
                PluginRemoting.interpolateString("${project.artifactId}suffix", "something"),
                is("somethingsuffix"));

        // no interpolation - contains neither ${artifactId} not ${project.artifactId}
        assertThat(
                PluginRemoting.interpolateString("${aartifactId}suffix", "something"),
                is("${aartifactId}suffix"));
        assertThat(
                PluginRemoting.interpolateString("${projectXartifactId}suffix", "something"),
                is("${projectXartifactId}suffix"));
    }

    @Test
    void smokes() throws Exception {
        File pomFile = new File(getClass().getResource("smokes/pom.xml").toURI());
        PluginRemoting pluginRemoting = new PluginRemoting(pomFile);
        PomData pomData = pluginRemoting.retrievePomData();
        assertThat(pomData.parent, nullValue());
        assertThat(pomData.groupId, is("com.example.jenkins"));
        assertThat(pomData.artifactId, is("example"));
        assertThat(pomData.getPackaging(), is("hpi"));
        assertThat(
                pomData.getConnectionUrl(),
                is("scm:git:https://jenkins.example.com/example-plugin.git"));
        assertThat(pomData.getScmTag(), is("example-4.1"));
    }

    @Test
    void parent() throws Exception {
        File pomFile = new File(getClass().getResource("parent/pom.xml").toURI());
        PluginRemoting pluginRemoting = new PluginRemoting(pomFile);
        PomData pomData = pluginRemoting.retrievePomData();
        assertThat(
                pomData.parent,
                is(new MavenCoordinates("com.example.jenkins", "example-parent", "4.1")));
        assertThat(pomData.groupId, nullValue());
        assertThat(pomData.artifactId, is("example"));
        assertThat(pomData.getPackaging(), is("hpi"));
        assertThat(
                pomData.getConnectionUrl(),
                is("scm:git:https://jenkins.example.com/example-plugin.git"));
        assertThat(pomData.getScmTag(), is("example-4.1"));
    }
}
