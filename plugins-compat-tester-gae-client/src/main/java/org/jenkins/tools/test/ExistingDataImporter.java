package org.jenkins.tools.test;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jenkins.tools.test.model.PluginCompatReport;
import org.jenkins.tools.test.model.PluginCompatResult;
import org.jenkins.tools.test.model.PluginInfos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author fcamblor
 */
public class ExistingDataImporter {
    public static void main(String[] args) throws IOException {
        File reportFile = new File(args[0]);
        new ExistingDataImporter(args[1], args[2]).importExistingReport(reportFile);
    }

    private String baseGAEUrl;
    private String securityToken;
    public ExistingDataImporter(String baseGAEUrl, String securityToken){
        this.baseGAEUrl = baseGAEUrl;
        this.securityToken = securityToken;
    }

    public void importPluginCompatResult(PluginInfos pluginInfos, PluginCompatResult pluginCompatResult) throws IOException {
        HttpClient httpClient = new HttpClient();
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
        String url = baseGAEUrl+"/writePctResult";

        HttpMethod method = new GetMethod(url);
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new NameValuePair("token", securityToken));
        parameters.add(new NameValuePair("pluginName", pluginInfos.pluginName));
        parameters.add(new NameValuePair("pluginVersion", pluginInfos.pluginVersion));
        parameters.add(new NameValuePair("pluginUrl", pluginInfos.pluginUrl));
        parameters.add(new NameValuePair("mavenGAV", pluginCompatResult.coreCoordinates.toGAV()));
        parameters.add(new NameValuePair("status", pluginCompatResult.status.name()));
        if(pluginCompatResult.compatTestExecutedOn != null){
            parameters.add(new NameValuePair("timestamp", String.valueOf(pluginCompatResult.compatTestExecutedOn.getTime())));
        }
        if(pluginCompatResult.errorMessage != null){
            parameters.add(new NameValuePair("errMsg", pluginCompatResult.errorMessage));
        }
        if(pluginCompatResult.warningMessages != null){
            for(String warnMsg : pluginCompatResult.warningMessages){
                parameters.add(new NameValuePair("warnMsgs", warnMsg));
            }
        }
        method.setQueryString(parameters.toArray(new NameValuePair[0]));

        httpClient.executeMethod(method);
    }

    public void importExistingReport(File reportFile) throws IOException {
        PluginCompatReport report = PluginCompatReport.fromXml(reportFile);

        int plannedRequestsCount = 0;
        for (Map.Entry<PluginInfos, List<PluginCompatResult>> test : report.getPluginCompatTests().entrySet()){
            plannedRequestsCount += test.getValue().size();
        }

        int i = 0;
        for (Map.Entry<PluginInfos, List<PluginCompatResult>> test : report.getPluginCompatTests().entrySet()){
            for (PluginCompatResult pluginCompatResult : test.getValue()) {
                importPluginCompatResult(test.getKey(), pluginCompatResult);
                i++;
                System.out.println(String.format("Executed request %d / %d", i, plannedRequestsCount));
            }
        }
    }
}
