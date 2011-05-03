package org.jenkins.tools.test.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PomExecutionException extends Exception {
    public final List<Throwable> exceptionsThrown;
    public final List<String> succeededPluginArtifactIds;
    public final List<String> pomWarningMessages;

    public PomExecutionException(PomExecutionException exceptionToCopy, List<String> succeededPluginArtifactIds, List<String> pomWarningMessages){
        this(exceptionToCopy.getMessage(), exceptionToCopy.exceptionsThrown);
        this.succeededPluginArtifactIds.addAll(succeededPluginArtifactIds);
        this.pomWarningMessages.addAll(pomWarningMessages);
    }

    public PomExecutionException(String message, List<Throwable> exceptionsThrown){
        super(message, exceptionsThrown.iterator().next());
        this.exceptionsThrown = exceptionsThrown;
        this.succeededPluginArtifactIds = new ArrayList<String>();
        this.pomWarningMessages = new ArrayList<String>();
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
}
