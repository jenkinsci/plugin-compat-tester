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
package org.jenkins.tools.test.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jenkins.tools.test.util.ExecutedTestNamesDetails;

/**
 * Exception thrown during a plugin's Maven execution
 *
 * @author Frederic Camblor
 */
public class PomExecutionException extends Exception {
    private final List<Throwable> exceptionsThrown;
    public final List<String> succeededPluginArtifactIds;
    private final List<String> pomWarningMessages;
    private final ExecutedTestNamesDetails testDetails;

    public PomExecutionException(Throwable cause) {
        this(cause.toString(), Collections.emptyList(), Collections.singletonList(cause), Collections.emptyList(), new ExecutedTestNamesDetails());
    }

    public PomExecutionException(PomExecutionException exceptionToCopy){
        this(exceptionToCopy.getMessage(), exceptionToCopy.succeededPluginArtifactIds, exceptionToCopy.exceptionsThrown, exceptionToCopy.pomWarningMessages, exceptionToCopy.testDetails);
    }

    public PomExecutionException(String message, List<String> succeededPluginArtifactIds, List<Throwable> exceptionsThrown, List<String> pomWarningMessages, ExecutedTestNamesDetails testDetails){
        super(message, exceptionsThrown.isEmpty() ? null : exceptionsThrown.iterator().next());
        this.exceptionsThrown = new ArrayList<>(exceptionsThrown);
        this.succeededPluginArtifactIds = new ArrayList<>(succeededPluginArtifactIds);
        this.pomWarningMessages = new ArrayList<>(pomWarningMessages);
        this.testDetails = testDetails;
    }

    public String getErrorMessage(){
        StringBuilder strBldr = new StringBuilder();
        strBldr.append(String.format("Message : %s %n %nExecuted plugins : %s %n %nStacktraces :%n",
                this.getMessage(), Arrays.toString(succeededPluginArtifactIds.toArray())));
        for(Throwable t : exceptionsThrown){
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            t.printStackTrace(printWriter);
            strBldr.append(String.format("%s %n %n", writer.toString()));
        }
        return strBldr.toString();
    }

    public List<String> getPomWarningMessages() {
        return pomWarningMessages;
    }
    
    public ExecutedTestNamesDetails getTestDetails() {
        return testDetails;
    }
}
