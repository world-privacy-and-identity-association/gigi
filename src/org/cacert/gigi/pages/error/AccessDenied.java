package org.cacert.gigi.pages.error;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.pages.Page;

public class AccessDenied extends Page {

    public AccessDenied() {
        super("Access denied");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        getDefaultTemplate().output(resp.getWriter(), Page.getLanguage(req), Collections.<String, Object>emptyMap());
    }

    @Override
    public boolean needsLogin() {
        return false;
    }

}
