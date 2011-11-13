package org.jenkins.tools.test.servlets;

import org.jenkins.tools.test.dao.PluginCompatResultDAO;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatReport;
import org.jenkins.tools.test.servlets.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author fcamblor
 */
public class RetrieveResultsServlet extends HttpServlet {

    private static class PluginMatcherFactory {
        public static PluginCompatResultDAO.PluginMatcher create(HttpServletRequest req){
            boolean everyPlugins = Boolean.valueOf(req.getParameter("everyPlugins")).booleanValue();

            PluginCompatResultDAO.PluginMatcher pluginMatcher = null;
            if(everyPlugins){
                pluginMatcher = PluginCompatResultDAO.PluginMatcher.All.INSTANCE;
            } else {
                // Fixing JQuery parameters encoding (it suffixes arrayed parameter with [])
                String[] paramValues = req.getParameterValues("plugins")!=null?req.getParameterValues("plugins"):req.getParameterValues("plugins[]");
                List<String> plugins = Arrays.asList(paramValues);
                pluginMatcher = new PluginCompatResultDAO.PluginMatcher.Parameterized(plugins);
            }
            return pluginMatcher;
        }
    }

    private static class CoreMatcherFactory {
        public static PluginCompatResultDAO.CoreMatcher create(HttpServletRequest req){
            boolean everyCores = Boolean.valueOf(req.getParameter("everyCores")).booleanValue();

            PluginCompatResultDAO.CoreMatcher coreMatcher = null;
            if(everyCores){
                coreMatcher = PluginCompatResultDAO.CoreMatcher.All.INSTANCE;
            } else {
                // Fixing JQuery parameters encoding (it suffixes arrayed parameter with [])
                String[] paramValues = req.getParameterValues("cores")!=null?req.getParameterValues("cores"):req.getParameterValues("cores[]");
                List<String> coreGAV = Arrays.asList(paramValues);
                // Converting GAVs into MavenCoordinates
                List<MavenCoordinates> coreCoords = new ArrayList<MavenCoordinates>(coreGAV.size());
                for(String gav : coreGAV){
                    coreCoords.add(MavenCoordinates.fromGAV(gav));
                }
                coreMatcher = new PluginCompatResultDAO.CoreMatcher.Parameterized(coreCoords);
            }
            return coreMatcher;
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        PluginCompatReport report = PluginCompatResultDAO.INSTANCE.search(
                PluginMatcherFactory.create(request), CoreMatcherFactory.create(request));

        response.setContentType("application/json");
        response.getWriter().println(JsonUtil.toJson(report));
    }
}
