package org.jenkins.tools.test.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Date;

public class PluginCompatResult {
    public final MavenCoordinates coreCoordinates;

    public final TestStatus status;
    public final Date compatTestExecutedOn;

    public final String errorMessage;

    public PluginCompatResult(MavenCoordinates coreCoordinates, TestStatus status,
                              String errorMessage){
        this.coreCoordinates = coreCoordinates;

        this.status = status;

        this.errorMessage = errorMessage;

        this.compatTestExecutedOn = new Date(); // now !
    }

    public boolean equals(Object o){
        if(o==null || !(o instanceof PluginCompatResult)){
            return false;
        }
        PluginCompatResult res = (PluginCompatResult)o;
        return new EqualsBuilder().append(coreCoordinates, res.coreCoordinates).isEquals();
    }

    public int hashCode(){
        return new HashCodeBuilder().append(coreCoordinates).toHashCode();
    }
}
