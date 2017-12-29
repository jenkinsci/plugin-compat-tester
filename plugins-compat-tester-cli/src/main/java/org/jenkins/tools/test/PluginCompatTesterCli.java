/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Erik Ramfelt, Koichi Fujikawa, Red Hat, Inc., Seiji Sogabe,
 * Stephen Connolly, Tom Huybrechts, Yahoo! Inc., Alan Harder, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkins.tools.test;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import hudson.maven.MavenEmbedderException;
import org.codehaus.plexus.PlexusContainerException;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.TestStatus;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Plugin compatibility tester frontend for the CLI
 * @author Frederic Camblor
 */
public class PluginCompatTesterCli {

    public static void main(String[] args) throws IOException, PlexusContainerException, MavenEmbedderException {
        CliOptions options = new CliOptions();
        JCommander jcommander = null;
        try {
            jcommander = new JCommander(options, args);
        }catch(ParameterException e){
            System.err.println(e.getMessage());
            if(jcommander == null){
                jcommander = new JCommander(options);
            }
            jcommander.usage();
            System.exit(1);
        }

        if (options.isPrintHelp()) {
            jcommander.usage();
            System.exit(0);
        }

        options.getWorkDirectory().mkdirs();

        File reportFile = null;
        if(!"NOREPORT".equals(options.getReportFile().getName())){
            reportFile = options.getReportFile();
        }
        if (reportFile != null) {
            // Check the format requirement
            File parentFile = reportFile.getParentFile();
            if (parentFile == null) {
                throw new IllegalArgumentException("The -reportFile value '" + reportFile + "' does not have a directory specification. " +
                        "A path should be something like 'out/pct-report.xml'");
            }
        }

        String updateCenterUrl = options.getUpdateCenterUrl();
        String parentCoordinates = options.getParentCoord();
        File war = options.getWar();
        if (war != null) {
            if (updateCenterUrl != null || parentCoordinates != null) {
                throw new IllegalStateException("Cannot specify -war together with either -updateCenterUrl or -parentCoordinates");
            }
        }

        // We may need this data even in the -war mode
        if (updateCenterUrl == null) {
            updateCenterUrl = PluginCompatTesterConfig.DEFAULT_UPDATE_CENTER_URL;
        }
        if (parentCoordinates == null) {
            parentCoordinates = PluginCompatTesterConfig.DEFAULT_PARENT_GAV;
        }

        PluginCompatTesterConfig config = new PluginCompatTesterConfig(updateCenterUrl, parentCoordinates,
                options.getWorkDirectory(), reportFile, options.getM2SettingsFile());
        config.setWar(war);

        config.setExternalMaven(options.getExternalMaven());

        if(options.getIncludePlugins() != null && !options.getIncludePlugins().isEmpty()){
            config.setIncludePlugins(Arrays.asList(options.getIncludePlugins().toLowerCase().split(",")));
        }
        if(options.getExcludePlugins() != null && !options.getExcludePlugins().isEmpty()){
            config.setExcludePlugins(Arrays.asList(options.getExcludePlugins().toLowerCase().split(",")));
        }
        if(options.getSkipTestCache() != null){
            config.setSkipTestCache(Boolean.valueOf(options.getSkipTestCache()).booleanValue());
        }
        if(options.getTestCacheTimeout() != null){
            config.setTestCacheTimeout(options.getTestCacheTimeout().longValue());
        }
        if(options.getCacheThresholdStatus() != null){
            config.setCacheThresholStatus(TestStatus.valueOf(options.getCacheThresholdStatus()));
        }
        if(options.getGaeBaseUrl() != null){
            config.setGaeBaseUrl(options.getGaeBaseUrl());
        }
        if(options.getGaeSecurityToken() != null){
            config.setGaeSecurityToken(options.getGaeSecurityToken());
        }
        if(options.getHookPrefixes() != null && !options.getHookPrefixes().isEmpty()){
            config.setHookPrefixes(Arrays.asList(options.getHookPrefixes().split(",")));
        }
        if(options.getLocalCheckoutDir() != null && !options.getLocalCheckoutDir().isEmpty()){
            config.setLocalCheckoutDir(options.getLocalCheckoutDir());
        }

        PluginCompatTester tester = new PluginCompatTester(config);
        tester.testPlugins();
    }
}
