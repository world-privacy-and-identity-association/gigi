package org.cacert.gigi;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

public class PolicyRedirector extends HandlerWrapper {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (target.equals("/")) {
            return;
        }
        if (target.equals("/policy/")) {
            return;
        }
        if (target.startsWith("/policy/") && target.endsWith(".php")) {
            target = target.replace(".php", ".html");
            response.sendRedirect(target);
            baseRequest.setHandled(true);
            return;
        }
        super.handle(target, baseRequest, request, response);
    }
}
