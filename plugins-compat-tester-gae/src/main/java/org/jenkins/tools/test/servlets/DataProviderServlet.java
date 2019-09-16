package org.jenkins.tools.test.servlets;

import org.jenkins.tools.test.dao.PluginCompatResultDAO;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.servlets.util.JsonUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.SortedSet;

/**
 * @author fcamblor
 */
public class DataProviderServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String type = request.getParameter("type");

        response.setContentType("application/json");
        Writer out = response.getWriter();
        if("cores".equals(type)){
            SortedSet<MavenCoordinates> cores = PluginCompatResultDAO.INSTANCE.findAllCores();
            out.write("{\"cores\":");
            JsonUtil.toJson(out, cores);
            out.write("}");
        } else if("pluginInfos".equals(type)){
            SortedSet<String> pluginInfoNames = PluginCompatResultDAO.INSTANCE.findAllPluginInfoNames();
            out.write("{");
            JsonUtil.displayMessages(out, "pluginInfos", pluginInfoNames);
            out.write("}");
        }

        out.flush();
    }
}
