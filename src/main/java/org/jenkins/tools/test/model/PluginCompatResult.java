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
package org.jenkins.tools.test.model;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * POJO storing a plugin compatibility test result
 *
 * @author Frederic Camblor
 */
public class PluginCompatResult implements Comparable<PluginCompatResult> {
    public final MavenCoordinates coreCoordinates;

    public final TestStatus status;
    public final Date compatTestExecutedOn;

    public final String errorMessage;

    private final Set<String> testDetails;

    private String buildLogPath = "";

    public PluginCompatResult(MavenCoordinates coreCoordinates, TestStatus status,
                              String errorMessage, Set<String> testDetails,
                              String buildLogPath){
        // Create new result with current date
        this(coreCoordinates, status, errorMessage, testDetails, buildLogPath, new Date());
    }
    private PluginCompatResult(MavenCoordinates coreCoordinates, TestStatus status,
                              String errorMessage, Set<String> testDetails,
                              String buildLogPath, Date compatTestExecutedOn){
        this.coreCoordinates = coreCoordinates;

        this.status = status;

        this.errorMessage = errorMessage;

        this.testDetails = testDetails;
        this.buildLogPath = buildLogPath;

        this.compatTestExecutedOn = compatTestExecutedOn;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PluginCompatResult res = (PluginCompatResult)o;
        return Objects.equals(coreCoordinates, res.coreCoordinates);
    }

    @Override
    public int hashCode(){
        return Objects.hash(coreCoordinates);
    }

    @Override
    public int compareTo(PluginCompatResult o) {
        return coreCoordinates.compareTo(o.coreCoordinates);
    }

    public String getBuildLogPath() {
        return buildLogPath;
    }

    public void setBuildLogPath(String buildLogPath) {
        this.buildLogPath = buildLogPath;
    }

    public Set<String> getTestsDetails() {
        return Collections.unmodifiableSet(testDetails);
    }
}