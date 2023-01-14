package org.jenkins.tools.test.model;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.util.VersionNumber;

public class PCTPlugin {
    private String name;
    private final String groupId;
    private VersionNumber version;

    public PCTPlugin(String name, String groupId, VersionNumber version) {
        this.name = name;
        this.groupId = groupId;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    @CheckForNull
    public String getGroupId() {
        return groupId;
    }

    public VersionNumber getVersion() {
        return version;
    }
}
