package org.jenkins.tools.test.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Date;

public class PluginCompatResult {
    public final String pluginName;
    public final String pluginVersion;
    public final String pluginUrl;

    public final boolean compilationOk;
    public final boolean testsOk;

    public final Date compatTestExecutedOn;

    public final String errorMessage;

    public PluginCompatResult(String pluginName, String pluginVersion, String pluginUrl,
                              boolean compilationOk, boolean testsOk, String errorMessage){
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
        this.pluginUrl = pluginUrl;

        this.compilationOk = compilationOk;
        this.testsOk = testsOk;

        this.errorMessage = errorMessage;

        this.compatTestExecutedOn = new Date(); // now !
    }

    public boolean equals(Object o){
        if(o==null || !(o instanceof PluginCompatResult)){
            return false;
        }
        PluginCompatResult res = (PluginCompatResult)o;
        return new EqualsBuilder().append(pluginName, res.pluginName).append(pluginVersion, res.pluginVersion).isEquals();
    }

    public int hashCode(){
        return new HashCodeBuilder().append(pluginName).append(pluginVersion).hashCode();
    }
}
