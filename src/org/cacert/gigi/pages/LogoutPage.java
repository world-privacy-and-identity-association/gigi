package org.cacert.gigi.pages;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.Gigi;
import org.cacert.gigi.dbObjects.User;

public class LogoutPage extends Page {

    public static final String PATH = "/logout";

    public LogoutPage(String title) {
        super(title);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession hs = req.getSession();
        if (req.getPathInfo() != null && req.getPathInfo().equals("/logout")) {
            if (hs != null) {
                hs.setAttribute(Gigi.LOGGEDIN, null);
                hs.invalidate();
            }
            resp.sendRedirect("/");
            return;
        }
    }

    @Override
    public boolean isPermitted(User u) {
        return u != null;
    }

}
