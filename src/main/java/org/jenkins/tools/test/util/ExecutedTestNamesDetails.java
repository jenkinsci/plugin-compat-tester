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
    }

    public void addFailedTest(String test) {
        add(FAILED, test);
    }

    public void addExecutedTest(String test) {
        add(EXECUTED, test);
    }

    public Set<String> getAll() {
        Set<String> result = new TreeSet<>();
        if (this.tests.containsKey(EXECUTED)) {
            result.addAll(this.tests.get(EXECUTED));
        }
        if (this.tests.containsKey(FAILED)) {
            result.addAll(this.tests.get(FAILED));
        }
        return Collections.unmodifiableSet(result);
    }

    public Set<String> getFailed() {
        return get(FAILED);
    }

    public Set<String> getExecuted() {
        return get(EXECUTED);
    }

    private Set<String> get(String key) {
        return this.tests.containsKey(key)
                ? Collections.unmodifiableSet(new TreeSet<>(this.tests.get(key)))
                : null;
    }

    private void add(String key, String test) {
        this.tests.computeIfAbsent(key, unused -> new TreeSet<>()).add(test);
    }

    public boolean hasBeenExecuted() {
        return getExecuted() != null || getFailed() != null;
    }

    public boolean isSuccess() {
        return getExecuted() != null && getFailed() == null;
    }

    public boolean hasFailures() {
        return getFailed() != null && !getFailed().isEmpty();
    }
}
