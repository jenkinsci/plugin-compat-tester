package org.jenkins.tools.test.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

public class PomExecutionException extends Exception {
    public final List<Throwable> exceptionsThrown;
    public final List<String> succeededPluginArtifactIds;

    public PomExecutionException(String message, List<Throwable> exceptionsThrown, List<String> succeededPluginArtifactIds){
        super(message, exceptionsThrown.iterator().next());
        this.exceptionsThrown = exceptionsThrown;
        this.succeededPluginArtifactIds = succeededPluginArtifactIds;
    }

    public String getErrorMessage(){
        StringBuilder strBldr = new StringBuilder();
        strBldr.append(String.format("Message : %1%n%nExecuted plugins : %2%n%nStacktraces :%n", this.getMessage(), succeededPluginArtifactIds.toArray()));
        for(Throwable t : exceptionsThrown){
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            t.printStackTrace(printWriter);
            strBldr.append(String.format("%1%n%n", writer.toString()));
        }
        return strBldr.toString();
    }
}
