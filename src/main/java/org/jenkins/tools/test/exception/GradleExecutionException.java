package org.jenkins.tools.test.exception;

public class GradleExecutionException extends PluginCompatibilityTesterException {

    private static final long serialVersionUID = 1L;

    public GradleExecutionException(String message) {
        super(message);
    }

    public GradleExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public GradleExecutionException(Throwable cause) {
        super(cause);
    }
}
