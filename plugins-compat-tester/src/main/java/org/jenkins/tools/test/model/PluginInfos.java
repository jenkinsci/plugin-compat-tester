package org.jenkins.tools.test.model;

import hudson.model.UpdateSite;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class PluginInfos {
    public final String pluginName;
    public final String pluginVersion;
    public final String pluginUrl;

    public PluginInfos(UpdateSite.Plugin plugin){
        this(plugin.name, plugin.version, plugin.url);
    }

    public PluginInfos(String pluginName, String pluginVersion, String pluginUrl){
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
        this.pluginUrl = pluginUrl;
    }

    public boolean equals(Object o){
        if(o==null || !(o instanceof PluginInfos)){
            return false;
        }
        PluginInfos infos = (PluginInfos)o;
        return new EqualsBuilder().append(pluginName, infos.pluginName).append(pluginVersion, infos.pluginVersion).isEquals();
    }

    public int hashCode(){
        return new HashCodeBuilder().append(pluginName).append(pluginVersion).toHashCode();
    }
}
