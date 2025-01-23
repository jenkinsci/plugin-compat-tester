package org.jenkins.tools.test.hook;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;

import hudson.util.VersionNumber;
import org.junit.jupiter.api.Test;

class JenkinsTestHarnessHookTest {
    @Test
    void nextVersion() {
        assertThat(
                JenkinsTestHarnessHook.determineNextVersion(new VersionNumber("2244")),
                hasToString("2244.2247.ve6b_a_8191b_95f"));
        assertThat(
                JenkinsTestHarnessHook.determineNextVersion(new VersionNumber("2270")),
                hasToString("2270.2272.vd890c8c611b_3"));
        assertThat(
                JenkinsTestHarnessHook.determineNextVersion(new VersionNumber("2271")),
                hasToString("2385.vfe86233d0d36"));
        assertThat(JenkinsTestHarnessHook.determineNextVersion(new VersionNumber("2386")), hasToString("2386"));
    }
}
