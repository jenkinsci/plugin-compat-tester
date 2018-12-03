package org.jenkins.tools.test.model;

import hudson.util.VersionNumber;

public class Plugin {
    private String name;
    private VersionNumber version;

    public Plugin(String name, VersionNumber version) {
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
