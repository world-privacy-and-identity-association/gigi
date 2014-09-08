package org.cacert.gigi.pages.account.certs;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.pages.Page;

public class CertificateAdd extends Page {

    public static final String PATH = "/account/certs/new";

    public CertificateAdd() {
        super("Create certificate");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new CertificateIssueForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CertificateIssueForm f = Form.getForm(req, CertificateIssueForm.class);
        if (f.submit(resp.getWriter(), req)) {
            Certificate c = f.getResult();
            String ser = c.getSerial();
            resp.sendRedirect(Certificates.PATH + "/" + ser);
        }
        f.output(resp.getWriter(), getLanguage(req), Collections.<String,Object>emptyMap());

    }
}
