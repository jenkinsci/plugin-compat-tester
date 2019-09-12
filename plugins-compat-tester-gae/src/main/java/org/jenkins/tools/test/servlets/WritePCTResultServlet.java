package org.jenkins.tools.test.servlets;

import com.google.appengine.api.datastore.Key;
import org.jenkins.tools.test.dao.LogsDAO;
import org.jenkins.tools.test.dao.PluginCompatResultDAO;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatResult;
import org.jenkins.tools.test.model.PluginInfos;
import org.jenkins.tools.test.model.TestStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author fcamblor
 */
public class WritePCTResultServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Current servlet is secured !
        SecuritySupport.ensureTokenIsValid(req);

        // Parsing request...
        String pluginName = req.getParameter("pluginName");
        String pluginVersion = req.getParameter("pluginVersion");
        String pluginUrl = req.getParameter("pluginUrl");
        TestStatus status = TestStatus.valueOf(req.getParameter("status"));
        String errorMessages = req.getParameter("errMsg");
        List<String> warningMessages = null;
        if(req.getParameterValues("warnMsgs") != null){
            warningMessages = Arrays.asList(req.getParameterValues("warnMsgs"));
        }
        String dateTimestampStr = req.getParameter("timestamp");
        Date date = null;
        if(dateTimestampStr == null){
            date = new Date(); // If date is not set, use NOW
        } else {
            date = new Date(Long.parseLong(dateTimestampStr));
        }
        String buildLogPath = req.getParameter("buildLogPath");

        PluginInfos pluginInfos = new PluginInfos(pluginName, pluginVersion, pluginUrl);
        MavenCoordinates mavenCoords = MavenCoordinates.fromGAV(req.getParameter("mavenGAV"));
        PluginCompatResult result = new PluginCompatResult(mavenCoords, status, errorMessages, warningMessages, buildLogPath, date);

        // Now, persisting result data into datastore !
        Key resultKey = PluginCompatResultDAO.INSTANCE.persist(pluginInfos, result);

        String logContent = req.getParameter("logContent");
        if(logContent != null && buildLogPath != null){
            LogsDAO.INSTANCE.persistBuildLog(buildLogPath, logContent, resultKey);
        }

        resp.getWriter().print(String.format("id=%d", resultKey.getId()));
    }
}
