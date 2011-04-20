package org.jenkins.tools.test.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class MavenCoordinates {
    public final String groupId;
    public final String artifactId;
    public final String version;

    public MavenCoordinates(String groupId, String artifactId, String version){
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public boolean equals(Object o){
        if(o==null || !(o instanceof MavenCoordinates)){
            return false;
        }
        MavenCoordinates c2 = (MavenCoordinates)o;
        return new EqualsBuilder().append(groupId, c2.groupId).append(artifactId, c2.artifactId).append(version, c2.version).isEquals();
    }

    public int hashCode(){
        return new HashCodeBuilder().append(groupId).append(artifactId).append(version).toHashCode();
    }
}
