package club.wpia.gigi.output;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class ClientCSRGenerate {

    private static final Template normal = new Template(ClientCSRGenerate.class.getResource("ClientCSRGenerate.templ"));

    public static void output(HttpServletRequest req, HttpServletResponse resp) {
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("minsize", "2048");
        vars.put("normalhost", "https://" + ServerConstants.getHostNamePortSecure(Host.WWW));
        vars.put("securehost", "https://" + ServerConstants.getHostNamePortSecure(Host.SECURE));
        vars.put("statichost", "https://" + ServerConstants.getHostNamePortSecure(Host.STATIC));
        try {
            normal.output(resp.getWriter(), Page.getLanguage(req), vars);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
