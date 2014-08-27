package org.cacert.gigi.output;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.ServerConstants;

public class ClientCSRGenerate {

    private static Template normal;

    private static Template IE;
    static {
        normal = new Template(ClientCSRGenerate.class.getResource("ClientCSRGenerate.templ"));
        IE = new Template(ClientCSRGenerate.class.getResource("ClientCSRGenerateIE.templ"));
    }

    public static void output(HttpServletRequest req, HttpServletResponse resp) {
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("minsize", "2048");
        vars.put("normalhost", "https://" + ServerConstants.getWwwHostNamePortSecure());
        vars.put("securehost", "https://" + ServerConstants.getSecureHostNamePort());
        vars.put("statichost", "https://" + ServerConstants.getStaticHostNamePortSecure());
        try {
            normal.output(resp.getWriter(), Page.getLanguage(req), vars);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
