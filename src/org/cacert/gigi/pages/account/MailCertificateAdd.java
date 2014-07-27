package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.Certificate;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.pages.Page;

public class MailCertificateAdd extends Page {

    public static final String PATH = "/account/certs/email/new";

    public MailCertificateAdd() {
        super("Create Email certificate");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new IssueCertificateForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        IssueCertificateForm f = Form.getForm(req, IssueCertificateForm.class);
        if (f.submit(resp.getWriter(), req)) {
            Certificate c = f.getResult();
            String ser = c.getSerial();
            resp.sendRedirect(MailCertificates.PATH + "/" + ser);
        }
        f.output(resp.getWriter(), getLanguage(req), Collections.<String,Object>emptyMap());

    }
}
