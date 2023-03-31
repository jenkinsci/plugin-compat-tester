package org.jenkins.tools.test.exception;

/** A {@ code RuntimeException} that wraps a wraps a PluginCompatibilityTesterException. */
public class WrappedPluginCompatabilityException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WrappedPluginCompatabilityException(PluginCompatibilityTesterException wrapped) {
        super(wrapped);
    }

    @SuppressWarnings("sync-override")
    @Override
    public PluginCompatibilityTesterException getCause() {
        return (PluginCompatibilityTesterException) super.getCause();
    }
}
