package org.jenkins.tools.test.exception;

import java.util.List;

public class PomExecutionException extends Exception {
    public final List<Throwable> exceptionsThrown;
    public final List<String> succeededPluginArtifactIds;

    public PomExecutionException(String message, List<Throwable> exceptionsThrown, List<String> succeededPluginArtifactIds){
        super(message, exceptionsThrown.iterator().next());
        this.exceptionsThrown = exceptionsThrown;
        this.succeededPluginArtifactIds = succeededPluginArtifactIds;
    }
}
