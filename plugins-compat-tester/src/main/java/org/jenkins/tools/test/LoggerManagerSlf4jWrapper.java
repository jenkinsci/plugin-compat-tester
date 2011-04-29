/**
 * Copyright - Accor - All Rights Reserved www.accorhotels.com
 */
package org.jenkins.tools.test;

import org.codehaus.plexus.logging.BaseLoggerManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.slf4j.Slf4jLogger;

/**
 * @author <a href="mailto:Olivier.LAMY@accor.com">Olivier Lamy</a>
 */
public class LoggerManagerSlf4jWrapper
    extends BaseLoggerManager
{

    protected Logger createLogger( String key )
    {
        return new Slf4jLogger( getThreshold(), org.slf4j.LoggerFactory.getLogger( key ) );
    }

}
