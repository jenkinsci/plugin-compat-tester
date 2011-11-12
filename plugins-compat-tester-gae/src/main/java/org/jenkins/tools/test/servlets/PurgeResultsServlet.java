package org.jenkins.tools.test.servlets;

import org.jenkins.tools.test.dao.PluginCompatResultDAO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author fcamblor
 */
public class PurgeResultsServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Current servlet is secured !
        SecuritySupport.ensureTokenIsValid(request);

        long deletedLines = PluginCompatResultDAO.INSTANCE.purgeResults();

        response.getWriter().println(String.format("%d lines deleted !", deletedLines));
    }
}
