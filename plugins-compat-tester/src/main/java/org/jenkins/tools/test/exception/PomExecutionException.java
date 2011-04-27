package org.jenkins.tools.test.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

public class PomExecutionException extends Exception {
    public final List<Throwable> exceptionsThrown;
    public final List<String> succeededPluginArtifactIds;
    private List<String> pomWarningMessages;

    public PomExecutionException(String message, List<Throwable> exceptionsThrown, List<String> succeededPluginArtifactIds){
        super(message, exceptionsThrown.iterator().next());
        this.exceptionsThrown = exceptionsThrown;
        this.succeededPluginArtifactIds = succeededPluginArtifactIds;
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

    public void setPomWarningMessages(List<String> pomWarningMessages) {
        this.pomWarningMessages = pomWarningMessages;
    }
}
