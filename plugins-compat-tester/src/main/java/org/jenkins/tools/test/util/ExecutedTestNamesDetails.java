package org.jenkins.tools.test.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ExecutedTestNamesDetails {
    
    private static final String FAILED = "FAILED";
    
    private static final String EXECUTED = "EXECUTED";
    
    private Map<String, Set<String>> tests;
    
    public ExecutedTestNamesDetails() {
        this.tests = new HashMap<>();
        this.tests.put(FAILED, new TreeSet<String>());
        this.tests.put(EXECUTED, new TreeSet<String>());
    }
    
    public void addFailedTest(String test) {
        add(FAILED, test);
    }
    
    public void addExecutedTest(String test) {
        add(EXECUTED, test);
    }
    
    public Set<String> getAll() {
        Set<String> result = new TreeSet<>(this.tests.get(EXECUTED));
        result.addAll(this.tests.get(FAILED));
        return Collections.unmodifiableSet(result);
    }
    
    public Set<String> getFailed() {
        return get(FAILED);
    }
    
    public Set<String> getExecuted() {
        return get(EXECUTED);
    }
    
    private Set<String> get(String key) {
        return Collections.unmodifiableSet(new TreeSet<>(this.tests.get(key)));
    }
    
    private void add(String key, String test) {
        this.tests.get(key).add(test);
    }
    
    

}
