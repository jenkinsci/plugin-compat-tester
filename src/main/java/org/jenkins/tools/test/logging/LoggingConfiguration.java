package org.jenkins.tools.test.logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.MissingResourceException;
import java.util.logging.LogManager;

/**
 * A logging configuration class suitable for passing to {@code -Djava.util.logging.config.class}.
 *
 * @see LogManager#readConfiguration()
 */
public class LoggingConfiguration {

    public LoggingConfiguration() {
        try (InputStream is = LoggingConfiguration.class.getResourceAsStream("logging.properties")) {
            if (is == null) {
                throw new MissingResourceException("Failed to load logging.properties", LoggingConfiguration.class.getName(), "logging.properties");
            }

            // Prefer new non-null values over old values.
            LogManager.getLogManager().updateConfiguration(is, property ->
                    ((oldValue, newValue) -> oldValue == null && newValue == null ? null : (newValue == null ? oldValue : newValue)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
