package org.jenkins.tools.test.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jenkins.tools.test.model.comparators.VersionComparator;

public class MavenCoordinates implements Comparable<MavenCoordinates> {
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

    public String toString(){
        return "MavenCoordinates[groupId="+groupId+", artifactId="+artifactId+", version="+version+"]";
    }

    public int compareTo(MavenCoordinates o) {
        if((groupId+":"+artifactId).equals(o.groupId+":"+o.artifactId)){
            return new VersionComparator().compare(version, o.version);
        } else {
            return (groupId+":"+artifactId).compareTo(o.groupId+":"+o.artifactId);
        }
    }
}
