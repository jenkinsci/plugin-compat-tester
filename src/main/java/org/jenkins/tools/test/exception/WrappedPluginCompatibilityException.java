package org.jenkins.tools.test.exception;

/** A {@code RuntimeException} that wraps a PluginCompatibilityTesterException. */
public class WrappedPluginCompatibilityException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WrappedPluginCompatibilityException(PluginCompatibilityTesterException wrapped) {
        super(wrapped);
    }

    @SuppressWarnings("sync-override")
    @Override
    public PluginCompatibilityTesterException getCause() {
        return (PluginCompatibilityTesterException) super.getCause();
    }
}
