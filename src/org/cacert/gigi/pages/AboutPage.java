package org.cacert.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.util.AuthorizationContext;

public class AboutPage extends Page {

    public AboutPage() {
        super("About");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        HashMap<String, Object> o = new HashMap<>();
        o.put("version", Package.getPackage("org.cacert.gigi").getImplementationVersion());
        getDefaultTemplate().output(out, getLanguage(req), o);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return true;
    }
}