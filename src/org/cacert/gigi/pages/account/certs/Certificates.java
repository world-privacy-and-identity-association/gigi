package org.cacert.gigi.pages.account.certs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.CACertificate;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.HandlesMixedRequest;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.PEM;

public class Certificates extends Page implements HandlesMixedRequest {

    private Template certDisplay = new Template(Certificates.class.getResource("CertificateDisplay.templ"));

    public static final String PATH = "/account/certs";

    static class TrustchainIterable implements IterableDataset {

        CACertificate cert;

        public TrustchainIterable(CACertificate cert) {
            this.cert = cert;
        }

        @Override
        public boolean next(Language l, Map<String, Object> vars) {
            if (cert == null) {
                return false;
            }
            vars.put("name", cert.getKeyname());
            vars.put("link", cert.getLink());
            if (cert.isSelfsigned()) {
                cert = null;
                return true;
            }
            cert = cert.getParent();
            return true;
        }

    }

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
            if (c == null || LoginPage.getAuthorizationContext(req).getTarget().getId() != c.getOwner().getId()) {
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
                if (req.getParameter("chain") != null) {
                    CACertificate ca = c.getParent();
                    while ( !ca.isSelfsigned()) {
                        out.println(PEM.encode("CERTIFICATE", ca.getCertificate().getEncoded()));
                        ca = ca.getParent();
                    }
                    if (req.getParameter("noAnchor") == null) {
                        out.println(PEM.encode("CERTIFICATE", ca.getCertificate().getEncoded()));
                    }
                }
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

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getQueryString() != null && !req.getQueryString().equals("") && !req.getQueryString().equals("withRevoked")) {
            return;// Block actions by get parameters.
        }
        if ( !req.getPathInfo().equals(PATH)) {
            resp.sendError(500);
            return;
        }
        Form.getForm(req, CertificateModificationForm.class).submit(resp.getWriter(), req);
        doGet(req, resp);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        String pi = req.getPathInfo().substring(PATH.length());
        if (pi.length() != 0) {
            pi = pi.substring(1);

            String serial = pi;
            Certificate c = Certificate.getBySerial(serial);
            if (c == null || LoginPage.getAuthorizationContext(req).getTarget().getId() != c.getOwner().getId()) {
                resp.sendError(404);
                return;
            }
            HashMap<String, Object> vars = new HashMap<>();
            vars.put("serial", URLEncoder.encode(serial, "UTF-8"));
            vars.put("trustchain", new TrustchainIterable(c.getParent()));
            try {
                vars.put("cert", c.cert());
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
            certDisplay.output(out, getLanguage(req), vars);

            return;
        }

        HashMap<String, Object> vars = new HashMap<String, Object>();
        new CertificateModificationForm(req, req.getParameter("withRevoked") != null).output(out, getLanguage(req), vars);
    }

}
