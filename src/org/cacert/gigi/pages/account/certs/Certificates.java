package org.cacert.gigi.pages.account.certs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.CertificateIterable;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.PEM;

public class Certificates extends Page {

    private Template certDisplay = new Template(Certificates.class.getResource("CertificateDisplay.templ"));

    public static final String PATH = "/account/certs";

    public Certificates() {
        super("Certificates");
    }

    @Override
    public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String pi = req.getPathInfo().substring(PATH.length());
        if (pi.length() == 0) {
            return false;
        }
        pi = pi.substring(1);
        boolean crt = false;
        boolean cer = false;
        resp.setContentType("application/pkix-cert");
        if (pi.endsWith(".crt")) {
            crt = true;
            pi = pi.substring(0, pi.length() - 4);
        } else if (pi.endsWith(".cer")) {
            if (req.getParameter("install") != null) {
                resp.setContentType("application/x-x509-user-cert");
            }
            cer = true;
            pi = pi.substring(0, pi.length() - 4);
        } else if (pi.endsWith(".cer")) {
            cer = true;
            pi = pi.substring(0, pi.length() - 4);
        }
        String serial = pi;
        try {
            Certificate c = Certificate.getBySerial(serial);
            if (c == null || getUser(req).getId() != c.getOwner().getId()) {
                resp.sendError(404);
                return true;
            }
            X509Certificate cert = c.cert();
            if ( !crt && !cer) {
                return false;
            }
            ServletOutputStream out = resp.getOutputStream();
            if (crt) {
                out.println(PEM.encode("CERTIFICATE", cert.getEncoded()));
            } else if (cer) {
                out.write(cert.getEncoded());
            }
        } catch (IllegalArgumentException e) {
            resp.sendError(404);
            return true;
        } catch (GeneralSecurityException e) {
            resp.sendError(404);
            return true;
        }

        return true;
    }

    private Template certTable = new Template(CertificateIterable.class.getResource("CertificateTable.templ"));

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        String pi = req.getPathInfo().substring(PATH.length());
        if (pi.length() != 0) {
            pi = pi.substring(1);

            String serial = pi;
            Certificate c = Certificate.getBySerial(serial);
            if (c == null || LoginPage.getUser(req).getId() != c.getOwner().getId()) {
                resp.sendError(404);
                return;
            }
            HashMap<String, Object> vars = new HashMap<>();
            vars.put("serial", URLEncoder.encode(serial, "UTF-8"));
            try {
                vars.put("cert", c.cert());
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
            certDisplay.output(out, getLanguage(req), vars);

            return;
        }

        HashMap<String, Object> vars = new HashMap<String, Object>();
        User us = LoginPage.getUser(req);
        vars.put("certs", new CertificateIterable(us.getCertificates(false)));
        certTable.output(out, getLanguage(req), vars);
    }

}
