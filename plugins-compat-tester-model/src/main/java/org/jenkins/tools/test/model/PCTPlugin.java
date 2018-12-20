package org.jenkins.tools.test.model;

import hudson.util.VersionNumber;

public class PCTPlugin {
    private String name;
    private VersionNumber version;

    public PCTPlugin(String name, VersionNumber version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public VersionNumber getVersion() {
        return version;
    }
}
