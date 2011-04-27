package org.jenkins.tools.test.model;

import org.apache.maven.execution.MavenExecutionResult;

import java.util.Collections;
import java.util.List;

public class TestExecutionResult {
    public final MavenExecutionResult mavenResult;
    public final List<String> pomWarningMessages;

    public TestExecutionResult(MavenExecutionResult mavenResult, List<String> pomWarningMessages){
        this.mavenResult = mavenResult;
        this.pomWarningMessages = Collections.unmodifiableList(pomWarningMessages);
    }
}
