package org.jenkins.tools.test;

import org.jenkins.tools.test.model.comparators.VersionComparator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
