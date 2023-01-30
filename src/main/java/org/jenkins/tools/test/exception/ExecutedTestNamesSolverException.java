package org.jenkins.tools.test.exception;

public class ExecutedTestNamesSolverException  extends Exception {

    private static final long serialVersionUID = 1L;

    public ExecutedTestNamesSolverException() {
            super();
    }

    public ExecutedTestNamesSolverException(String msg) {
            super(msg);
    }

    public ExecutedTestNamesSolverException(String msg, Exception e) {
            super(msg, e);
    }

    public ExecutedTestNamesSolverException(Exception e) {
            super(e);
    }
}
