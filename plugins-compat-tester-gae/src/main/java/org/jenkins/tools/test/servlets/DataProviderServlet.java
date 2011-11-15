package org.jenkins.tools.test.servlets;

import org.jenkins.tools.test.dao.PluginCompatResultDAO;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.servlets.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.SortedSet;

/**
 * @author fcamblor
 */
public class DataProviderServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String type = request.getParameter("type");

        String jsonToWrite = "{ }";
        if("cores".equals(type)){
            SortedSet<MavenCoordinates> cores = PluginCompatResultDAO.INSTANCE.findAllCores();
            StringBuilder sb = new StringBuilder();
            JsonUtil.toJson(sb, cores);
            jsonToWrite = String.format("{\"cores\":%s}", sb.toString());
        } else if("pluginInfos".equals(type)){
            SortedSet<String> pluginInfoNames = PluginCompatResultDAO.INSTANCE.findAllPluginInfoNames();
            StringBuilder sb = new StringBuilder();
            JsonUtil.displayMessages(sb, "pluginInfos", pluginInfoNames);
            jsonToWrite = String.format("{%s}", sb.toString());
        }

        response.setContentType("application/json");
        response.getWriter().append(jsonToWrite);
        response.getWriter().flush();
    }
}
