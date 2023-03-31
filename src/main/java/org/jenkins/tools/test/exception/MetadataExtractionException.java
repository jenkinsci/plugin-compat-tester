package org.jenkins.tools.test.exception;

/**
 * Exception used when extracting metadata from the war fails, e.g. the list of plugins or Jenkins
 * version could not be obtained.
 */
public class MetadataExtractionException extends PluginCompatibilityTesterException {

    private static final long serialVersionUID = 1L;

    public MetadataExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetadataExtractionException(String message) {
        super(message);
    }
}
