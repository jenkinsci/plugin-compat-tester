package org.jenkins.tools.test.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

public class PluginRemotingTest {

    @Test
    public void testStringInterpolation() {
        assertThat(
                PluginRemoting.interpolateString("${project.artifactId}", "wibble"), is("wibble"));
        assertThat(
                PluginRemoting.interpolateString("prefix-${project.artifactId}", "blah"),
                is("prefix-blah"));
        assertThat(
                PluginRemoting.interpolateString("${project.artifactId}suffix", "something"),
                is("somethingsuffix"));

        // no interpolation - contains neither ${artifactId} not ${project.artifactId}
        assertThat(PluginRemoting.interpolateString(null, "wibble"), nullValue());
        assertThat(
                PluginRemoting.interpolateString("${aartifactId}suffix", "something"),
                is("${aartifactId}suffix"));
        assertThat(
                PluginRemoting.interpolateString("${projectXartifactId}suffix", "something"),
                is("${projectXartifactId}suffix"));
    }
}
