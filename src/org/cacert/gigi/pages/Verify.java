package org.cacert.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Verifyable;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;

public class Verify extends Page {

    private class VerificationForm extends Form {

        private String hash;

        private String type;

        private String id;

        private Verifyable target;

        public VerificationForm(HttpServletRequest hsr) {
            super(hsr, PATH);
            hash = hsr.getParameter("hash");
            type = hsr.getParameter("type");
            id = hsr.getParameter("id");
            if ("email".equals(type)) {
                target = EmailAddress.getById(Integer.parseInt(id));
            } else if ("domain".equals("type")) {
                target = Domain.getById(Integer.parseInt(id));
            }
        }

        @Override
        public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
            if ("email".equals(type)) {
                try {
                    target.verify(hash);
                    out.println("Email verification completed.");
                } catch (IllegalArgumentException e) {
                    out.println(translate(req, "The email address is invalid."));
                } catch (GigiApiException e) {
                    e.format(out, getLanguage(req));
                }
            } else if ("domain".equals(type)) {
                try {
                    target.verify(hash);
                    out.println("Domain verification completed.");
                } catch (IllegalArgumentException e) {
                    out.println(translate(req, "The domain is invalid."));
                } catch (GigiApiException e) {
                    e.format(out, getLanguage(req));
                }
            }
            return true;
        }

        @Override
        protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
            vars.put("hash", hash);
            vars.put("id", id);
            vars.put("type", type);
            if (target instanceof EmailAddress) {
                vars.put("subject", ((EmailAddress) target).getAddress());
            } else if (target instanceof Domain) {
                vars.put("subject", ((Domain) target).getSuffix());
            }
            getDefaultTemplate().output(out, l, vars);
        }
    }

    public static final String PATH = "/verify";

    public Verify() {
        super("Verify email");
    }

    @Override
    public boolean needsLogin() {
        return false;
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (Form.getForm(req, VerificationForm.class).submit(resp.getWriter(), req)) {
            }
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            new VerificationForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        } catch (IllegalArgumentException e) {
            resp.getWriter().println(translate(req, "The object to verify is invalid."));

        }
    }

}
