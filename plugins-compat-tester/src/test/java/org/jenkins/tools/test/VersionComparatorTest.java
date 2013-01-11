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

import org.jenkins.tools.test.model.comparators.VersionComparator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for plugin version comparisons
 * @author Frederic Camblor
 */
public class VersionComparatorTest {

    private static final Map<String, Integer> OPERAND_CONVERSION = new HashMap<String, Integer>(){{
       put("<", Integer.valueOf(-1)); put("=", Integer.valueOf(0)); put(">", Integer.valueOf(1));
    }};


    private void test(String v1, String operator, String v2){
        test(v1, OPERAND_CONVERSION.get(operator).intValue(), v2);
    }

    private void test(String v1, int compResult, String v2){
        assertThat(new VersionComparator().compare(v1, v2), is(equalTo(compResult)));
    }

    private void testAndCommutate(String v1, String operand, String v2){
        test(v1, OPERAND_CONVERSION.get(operand).intValue(), v2);
        test(v2, OPERAND_CONVERSION.get(operand).intValue()*-1, v1);
    }

    @Test
    public void shouldBasicEqualComparisonTestBeOk(){
        test("1.2.3", "=", "1.2.3");
        test("1", "=", "1");
    }

    @Test
    public void shouldBasicNonEqualComparisonTestBeOk(){
        testAndCommutate("1", "<", "2");
        testAndCommutate("10", ">", "2");
        testAndCommutate("1.2", "<", "2.1");
        testAndCommutate("1.1", "<", "1.2");
        testAndCommutate("1.1.2", "<", "1.2.1");
        testAndCommutate("1.10.2", ">", "1.2.1");
    }

    @Test
    public void shouldSpecialCasesBeHandledCorrectly(){
        test("1", "<", "1.2.1");
        test("1.2", "<", "1.2.1");
        test("1.1-beta", ">", "1.1-alpha");
        test("1.1-beta", "<", "1.2");
        test("1.1-beta1", "<", "1.1-beta2");
    }
}
