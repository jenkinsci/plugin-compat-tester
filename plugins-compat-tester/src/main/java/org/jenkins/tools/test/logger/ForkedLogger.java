package org.jenkins.tools.test.logger;

import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;

import java.util.List;

/**
 * Class used to simultaneously log thinkgs into several runtime loggers
 * @author Frederic Camblor
 */
public class ForkedLogger extends AbstractLogger {

    private List<Logger> loggers;

    public ForkedLogger(List<Logger> loggers){
        this(loggers, Logger.LEVEL_INFO);
    }

    public ForkedLogger(List<Logger> loggers, final int threshold){
        this(loggers, threshold, "default");
    }

    public ForkedLogger(List<Logger> loggers, final int threshold, final String name){
        super(threshold, name);
        this.loggers = loggers;
        for(Logger l : loggers){
            l.setThreshold(threshold);
        }
    }

    public void debug(String message, Throwable throwable) {
        for(Logger l : loggers){
            l.debug(message, throwable);
        }
    }

    public void info(String message, Throwable throwable) {
        for(Logger l : loggers){
            l.info(message, throwable);
        }
    }

    public void warn(String message, Throwable throwable) {
        for(Logger l : loggers){
            l.warn(message, throwable);
        }
    }

    public void error(String message, Throwable throwable) {
        for(Logger l : loggers){
            l.error(message, throwable);
        }
    }

    public void fatalError(String message, Throwable throwable) {
        for(Logger l : loggers){
            l.fatalError(message, throwable);
        }
    }

    public Logger getChildLogger(String name) {
        return this;
    }


}
