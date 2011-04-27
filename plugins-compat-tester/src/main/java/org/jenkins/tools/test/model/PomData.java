package org.jenkins.tools.test.model;

import java.util.ArrayList;
import java.util.List;

public class PomData {
    public final String artifactId;
    private String connectionUrl;
    private List<String> warningMessages = new ArrayList<String>();

    public PomData(String artifactId, String connectionUrl){
        this.artifactId = artifactId;
        this.setConnectionUrl(connectionUrl);
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public List<String> getWarningMessages() {
        return warningMessages;
    }

    public void setWarningMessages(List<String> warningMessages) {
        this.warningMessages = warningMessages;
    }
}
