package org.jenkins.tools.test.hook;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;

import hudson.util.VersionNumber;
import org.junit.jupiter.api.Test;

class JenkinsTestHarnessHookTest {
    @Test
    void nextVersion() {
        assertThat(
                JenkinsTestHarnessHook.determineNextVersion(new VersionNumber("2243")),
                hasToString(JenkinsTestHarnessHook.VERSION_BACKPORT_2244));
        assertThat(
                JenkinsTestHarnessHook.determineNextVersion(new VersionNumber("2244")),
                hasToString(JenkinsTestHarnessHook.VERSION_BACKPORT_2244));
        assertThat(
                JenkinsTestHarnessHook.determineNextVersion(new VersionNumber("2269")),
                hasToString(JenkinsTestHarnessHook.VERSION_BACKPORT_2270));
        assertThat(
                JenkinsTestHarnessHook.determineNextVersion(new VersionNumber("2270")),
                hasToString(JenkinsTestHarnessHook.VERSION_BACKPORT_2270));
        assertThat(
                JenkinsTestHarnessHook.determineNextVersion(new VersionNumber("2271")),
                hasToString(JenkinsTestHarnessHook.VERSION_WITH_WEB_FRAGMENTS));
        // assertThat(JenkinsTestHarnessHook.determineNextVersion(new VersionNumber("2387")), hasToString("2387"));
    }
}
