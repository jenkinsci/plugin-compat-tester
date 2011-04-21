package org.jenkins.tools.test.model;

public class PomData {
    public final String artifactId;
    public final String connectionUrl;

    public PomData(String artifactId, String connectionUrl){
        this.artifactId = artifactId;
        this.connectionUrl = connectionUrl;
    }
}
