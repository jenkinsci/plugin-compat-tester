package org.jenkins.tools.test.servlets;

import org.jenkins.tools.test.dao.SecurityTokenDAO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author fcamblor
 */
public class SecuritySupport {

    public static void ensureTokenIsValid(HttpServletRequest req){
        String token = req.getParameter("token");
        if(token == null){
            throw new IllegalArgumentException("Field [token] is required for current request !");
        }

        if(!SecurityTokenDAO.INSTANCE.isTokenValid(token)){
            throw new IllegalStateException("Your token is invalid : we cannot go further from there !");
        }
    }
}
