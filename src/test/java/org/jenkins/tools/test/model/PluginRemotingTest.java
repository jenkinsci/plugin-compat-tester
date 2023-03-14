package org.jenkins.tools.test.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import org.apache.maven.model.Model;
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
        Model model = pluginRemoting.retrieveModel();
        assertThat(model.getParent(), nullValue());
        assertThat(model.getGroupId(), is("com.example.jenkins"));
        assertThat(model.getArtifactId(), is("example"));
        assertThat(model.getPackaging(), is("hpi"));
        assertThat(
                model.getScm().getConnection(),
                is("scm:git:https://jenkins.example.com/example-plugin.git"));
        assertThat(model.getScm().getTag(), is("example-4.1"));
    }

    @Test
    void parent() throws Exception {
        File pomFile = new File(getClass().getResource("parent/pom.xml").toURI());
        PluginRemoting pluginRemoting = new PluginRemoting(pomFile);
        Model model = pluginRemoting.retrieveModel();
        assertThat(model.getParent().getGroupId(), is("com.example.jenkins"));
        assertThat(model.getParent().getArtifactId(), is("example-parent"));
        assertThat(model.getParent().getVersion(), is("4.1"));
        assertThat(model.getGroupId(), nullValue());
        assertThat(model.getArtifactId(), is("example"));
        assertThat(model.getPackaging(), is("hpi"));
        assertThat(
                model.getScm().getConnection(),
                is("scm:git:https://jenkins.example.com/example-plugin.git"));
        assertThat(model.getScm().getTag(), is("example-4.1"));
    }

    @Test
    void negative() throws Exception {
        File pomFile = new File(getClass().getResource("negative/pom.xml").toURI());
        PluginRemoting pluginRemoting = new PluginRemoting(pomFile);
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, pluginRemoting::retrieveModel);
        assertThat(e.getMessage(), is("Failed to parse pom.xml"));
    }
}
