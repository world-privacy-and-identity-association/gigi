package org.cacert.gigi.pages;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.output.template.Template;

public class MainPage extends Page {

    Template notLog = new Template(MainPage.class.getResource("MainPageNotLogin.templ"));

    public MainPage() {
        super("Home");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (LoginPage.getUser(req) != null) {
            getDefaultTemplate().output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        } else {
            notLog.output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        }
    }

    @Override
    public boolean needsLogin() {
        return false;
    }
}
