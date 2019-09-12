package org.jenkins.tools.test.servlets;

import org.jenkins.tools.test.dao.SecurityTokenDAO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author fcamblor
 * Servlet allowing to create a fake token in database to initiate
 * entity
 */
public class InitTokenServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SecurityTokenDAO.INSTANCE.initializeUniqueToken();
        resp.getWriter().println("Token generated successfully !");
    }
}
