package org.jenkins.tools.test.exception;

/**
 * Superclass for errors that can occur when a compatibility test fails. These should normally
 * bubble up to the main method and cause the run to fail unless the "fail fast" option is disabled,
 * in which case additional plugins will be tested and a combined exception thrown after all plugins
 * have been tested.
 */
public class PluginCompatibilityTesterException extends Exception {

    private static final long serialVersionUID = 1L;

    public PluginCompatibilityTesterException(String message) {
        super(message);
    }

    public PluginCompatibilityTesterException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginCompatibilityTesterException(Throwable cause) {
        super(cause);
    }
}
