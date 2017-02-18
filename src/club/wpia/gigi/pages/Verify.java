package club.wpia.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.EmailAddress;
import club.wpia.gigi.dbObjects.Verifyable;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Scope;
import club.wpia.gigi.output.template.SprintfCommand;

public class Verify extends Page {

    private static final SprintfCommand emailAddressVerified = new SprintfCommand("Email address {0} verified", Arrays.asList("${subject"));

    private static final SprintfCommand domainVerified = new SprintfCommand("Domain {0} verified", Arrays.asList("${subject"));

    private class VerificationForm extends Form {

        private String hash;

        private String type;

        private String id;

        private Verifyable target;

        String subject;

        public VerificationForm(HttpServletRequest hsr) {
            super(hsr, PATH);
            hash = hsr.getParameter("hash");
            type = hsr.getParameter("type");
            id = hsr.getParameter("id");
            if ("email".equals(type)) {
                EmailAddress addr = EmailAddress.getById(Integer.parseInt(id));
                subject = addr.getAddress();
                target = addr;
            } else if ("domain".equals(type)) {
                Domain domain = Domain.getById(Integer.parseInt(id));
                subject = domain.getSuffix();
                target = domain;
            } else {
                throw new IllegalArgumentException();
            }
            try {
                if ( !target.isVerifyable(hash)) {
                    throw new IllegalArgumentException();
                }
            } catch (GigiApiException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
            HashMap<String, Object> data = new HashMap<>();
            data.put("subject", subject);
            if ("email".equals(type)) {
                try {
                    target.verify(hash);
                } catch (IllegalArgumentException e) {
                    throw new PermamentFormException(new GigiApiException("Given token could not be found to complete the verification process (Email Ping)."));
                }
                return new SuccessMessageResult(new Scope(emailAddressVerified, data));
            } else if ("domain".equals(type)) {
                try {
                    target.verify(hash);
                } catch (IllegalArgumentException e) {
                    throw new PermamentFormException(new GigiApiException("Given token could not be found to complete the verification process (Domain Ping)."));
                }
                return new SuccessMessageResult(new Scope(domainVerified, data));
            } else {
                throw new GigiApiException("Invalid object type.");
            }
        }

        @Override
        protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
            vars.put("hash", hash);
            vars.put("id", id);
            vars.put("type", type);

            vars.put("subject", subject);
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
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return Form.getForm(req, VerificationForm.class).submitExceptionProtected(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            Form.getForm(req, VerificationForm.class).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
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
