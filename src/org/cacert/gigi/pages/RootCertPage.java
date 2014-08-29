package org.cacert.gigi.pages;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.HashMap;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.util.PEM;

public class RootCertPage extends Page {

    private Certificate root;

    public RootCertPage(KeyStore ks) {
        super("Root Certificates");
        try {
            root = ks.getCertificate("root");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("pem") != null && root != null) {
            resp.setContentType("application/x-x509-ca-cert");
            ServletOutputStream out = resp.getOutputStream();
            try {
                out.println(PEM.encode("CERTIFICATE", root.getEncoded()));
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
            return true;
        } else if (req.getParameter("cer") != null && root != null) {
            resp.setContentType("application/x-x509-ca-cert");
            ServletOutputStream out = resp.getOutputStream();
            try {
                out.write(root.getEncoded());
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());

    }

    @Override
    public boolean needsLogin() {
        return false;
    }

}
