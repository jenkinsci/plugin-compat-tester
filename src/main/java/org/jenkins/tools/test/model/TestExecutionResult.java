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
import java.util.List;
import java.util.Set;

import org.jenkins.tools.test.util.ExecutedTestNamesDetails;

/**
 * POJO containing plugin compatibility test result
 *
 * @author Frederic Camblor
 */
public class TestExecutionResult {

    private final ExecutedTestNamesDetails testDetails;

    public final List<String> pomWarningMessages;

    public TestExecutionResult(List<String> pomWarningMessages){
        this(pomWarningMessages, new ExecutedTestNamesDetails());
    }

    public TestExecutionResult(List<String> pomWarningMessages, ExecutedTestNamesDetails testDetails){
        this.pomWarningMessages = Collections.unmodifiableList(pomWarningMessages);
        this.testDetails = testDetails;
    }

    public ExecutedTestNamesDetails getTestDetails() {
        return testDetails;
    }
}