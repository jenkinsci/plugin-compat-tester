package org.jenkins.tools.test.model;

/**
 * Created by IntelliJ IDEA.
 * User: Fred
 * Date: 20/04/11
 * Time: 00:38
 * To change this template use File | Settings | File Templates.
 */
public class PluginCompatResult {
    public final String coreGroupId;
    public final String coreArtifactId;
    public final String coreVersion;

    public final boolean compilationOk;
    public final boolean testsOk;

    public final String errorMessage;

    public PluginCompatResult(String coreGroupId, String coreArtifactId, String coreVersion,
                              boolean compilationOk, boolean testsOk, String errorMessage){
        this.coreGroupId = coreGroupId;
        this.coreArtifactId = coreArtifactId;
        this.coreVersion = coreVersion;

        this.compilationOk = compilationOk;
        this.testsOk = testsOk;

        this.errorMessage = errorMessage;
    }
}
