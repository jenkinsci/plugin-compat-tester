package org.jenkins.tools.test;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.cli.PrintStreamLogger;
import org.codehaus.plexus.logging.BaseLoggerManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.slf4j.Slf4jLogger;
import org.jenkins.tools.test.logger.ForkedLogger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy & Frederic Camblor
 */
public class PluginCompatTesterLoggerManager extends BaseLoggerManager
{

    private File logFile;

    public PluginCompatTesterLoggerManager(){
        this(null);
    }

    public PluginCompatTesterLoggerManager(File logFile){
        this.logFile = logFile;
    }

    @Override
    protected Logger createLogger(String name) {
        List<Logger> loggers = new ArrayList();
        try {
            if(logFile != null){
                loggers.add(new PrintStreamLogger(new PrintStream(logFile)));
            }
        } catch (FileNotFoundException e) {
            new IllegalStateException(e);
        }
        loggers.add(new Slf4jLogger( getThreshold(), LoggerFactory.getLogger( name ) ));
        return new ForkedLogger(loggers, getThreshold(), name);
    }
}
