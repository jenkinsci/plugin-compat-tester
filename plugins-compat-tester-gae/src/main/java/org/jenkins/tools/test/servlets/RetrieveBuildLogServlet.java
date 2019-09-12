package org.jenkins.tools.test.servlets;

import org.jenkins.tools.test.dao.LogsDAO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author fcamblor
 */
public class RetrieveBuildLogServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String buildLogPath = request.getParameter("buildLogPath");

        String logContent = LogsDAO.INSTANCE.getLogContent(buildLogPath);

        response.getWriter().append(logContent);
        response.getWriter().flush();
    }
}
