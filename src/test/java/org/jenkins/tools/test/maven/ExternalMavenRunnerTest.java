package org.jenkins.tools.test.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExternalMavenRunnerTest {

    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    @ParameterizedTest
    @CsvSource({
        "Windows,mvn.cmd",
        "Windows 11,mvn.cmd",
        "Windows Server 2022,mvn.cmd",
        "Linux,mvn",
        "Mac OS X,mvn",
        "Darwin,mvn",
        "windows 11,mvn",
        "'',mvn",
        ",mvn"
    })
    void selectsDefaultExecutable(String osName, String expected) {
        String previous = System.getProperty("os.name");
        try {
            setOsName(osName);
            assertEquals(expected, ExternalMavenRunner.defaultMavenExecutable());
        } finally {
            setOsName(previous);
        }
    }

    private static void setOsName(String name) {
        if (name == null) {
            System.clearProperty("os.name");
        } else {
            System.setProperty("os.name", name);
        }
    }
}
