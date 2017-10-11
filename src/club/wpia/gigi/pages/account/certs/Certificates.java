package club.wpia.gigi.pages.account.certs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.Certificate.SubjectAlternateName;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.SupportedUser;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.TrustchainIterable;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.HandlesMixedRequest;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.CertExporter;
import club.wpia.gigi.util.PEM;

public class Certificates extends Page implements HandlesMixedRequest {

    private static final Template certDisplay = new Template(Certificates.class.getResource("CertificateDisplay.templ"));

    public static final String PATH = "/account/certs";

    public static final String SUPPORT_PATH = "/support/certs";

    private final boolean support;

    public Certificates(boolean support) {
        super(support ? "Support Certificates" : "Certificates");
        this.support = support;
    }

    @Override
    public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("POST".equals(req.getMethod())) {
            return beforePost(req, resp);
        }

        String pi = req.getPathInfo().substring(PATH.length());
        if (pi.length() == 0) {
            return false;
        }
        pi = pi.substring(1);
        boolean crt = false;
        boolean cer = false;
        resp.setContentType("application/pkix-cert");
        if (req.getParameter("install") != null) {
            resp.setContentType("application/x-x509-user-cert");
        }
        if (pi.endsWith(".crt")) {
            crt = true;
            pi = pi.substring(0, pi.length() - 4);
        } else if (pi.endsWith(".cer")) {
            cer = true;
            pi = pi.substring(0, pi.length() - 4);
        }
        String serial = pi;
        try {
            Certificate c = Certificate.getBySerial(serial);
            if (c == null || ( !support && LoginPage.getAuthorizationContext(req).getTarget().getId() != c.getOwner().getId())) {
                resp.sendError(404);
                return true;
            }
            if ( !crt && !cer) {
                return false;
            }
            ServletOutputStream out = resp.getOutputStream();
            boolean doChain = req.getParameter("chain") != null;
            boolean includeAnchor = req.getParameter("noAnchor") == null;
            boolean includeLeaf = req.getParameter("noLeaf") == null;
            if (crt) {
                CertExporter.writeCertCrt(c, out, doChain, includeAnchor, includeLeaf);
            } else if (cer) {
                CertExporter.writeCertCer(c, out, doChain, includeAnchor);
            }
        } catch (IllegalArgumentException e) {
            resp.sendError(404);
            return true;
        } catch (GigiApiException e) {
            resp.sendError(404);
            return true;
        } catch (GeneralSecurityException e) {
            resp.sendError(404);
            return true;
        }

        return true;
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (support && "revoke".equals(req.getParameter("action"))) {
            return Form.getForm(req, RevokeSingleCertForm.class).submitExceptionProtected(req, resp);
        }
        if ( !req.getPathInfo().equals(PATH)) {
            resp.sendError(500);
            return true;
        }
        return Form.getForm(req, CertificateModificationForm.class).submitExceptionProtected(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getQueryString() != null && !req.getQueryString().equals("") && !req.getQueryString().equals("withRevoked")) {
            return;// Block actions by get parameters.
        }

        if (support && "revoke".equals(req.getParameter("action"))) {
            if (Form.printFormErrors(req, resp.getWriter())) {
                Form.getForm(req, RevokeSingleCertForm.class).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
            }
            return;
        }
        if ( !req.getPathInfo().equals(PATH)) {
            resp.sendError(500);
            return;
        }
        Form.getForm(req, CertificateModificationForm.class).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        String pi = req.getPathInfo().substring(PATH.length());
        if (pi.length() != 0) {
            pi = pi.substring(1);

            String serial = pi;
            Certificate c = Certificate.getBySerial(serial);
            Language l = LoginPage.getLanguage(req);

            if (c == null || ( !support && LoginPage.getAuthorizationContext(req).getTarget().getId() != c.getOwner().getId())) {
                resp.sendError(404);
                return;
            }
            Map<String, Object> vars = getDefaultVars(req);
            vars.put("serial", URLEncoder.encode(serial, "UTF-8"));

            CertificateStatus st = c.getStatus();

            if (support) {
                vars.put("support", "support");
                CertificateOwner user = c.getOwner();
                if (st == CertificateStatus.ISSUED) {
                    if (user instanceof User) {
                        vars.put("revokeForm", new RevokeSingleCertForm(req, c, new SupportedUser((User) user, getUser(req), LoginPage.getAuthorizationContext(req).getSupporterTicketId())));
                    }
                }
            }

            CertificateOwner co = c.getOwner();
            int ownerId = co.getId();
            vars.put("certid", c.getStatus());
            if (co instanceof Organisation) {
                vars.put("type", l.getTranslation("Organisation Acount"));
                vars.put("name", Organisation.getById(ownerId).getName());
                vars.put("link", ""); // TODO
            } else {
                vars.put("type", l.getTranslation("Personal Account"));
                vars.put("name", User.getById(ownerId).getPreferredName());
                vars.put("link", "/support/user/" + ownerId + "/");
            }
            vars.put("status", c.getStatus());
            vars.put("DN", c.getDistinguishedName());
            vars.put("digest", c.getMessageDigest());
            vars.put("profile", c.getProfile().getVisibleName());
            vars.put("fingerprint", "TBD"); // TODO function needs to be
                                            // implemented in Certificate.java
            try {

                if (st == CertificateStatus.ISSUED || st == CertificateStatus.REVOKED) {
                    X509Certificate certx = c.cert();
                    vars.put("issued", certx.getNotBefore());
                    vars.put("expire", certx.getNotAfter());
                    vars.put("cert", PEM.encode("CERTIFICATE", c.cert().getEncoded()));
                } else {
                    vars.put("issued", l.getTranslation("N/A"));
                    vars.put("expire", l.getTranslation("N/A"));
                    vars.put("cert", l.getTranslation("N/A"));
                }
                if (st == CertificateStatus.REVOKED) {
                    vars.put("revoked", c.getRevocationDate());
                } else {
                    vars.put("revoked", l.getTranslation("N/A"));
                }
                if (st == CertificateStatus.ISSUED || st == CertificateStatus.REVOKED) {
                    vars.put("trustchain", new TrustchainIterable(c.getParent()));
                    try {
                        vars.put("cert", PEM.encode("CERTIFICATE", c.cert().getEncoded()));
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                    }
                } else {
                    vars.put("trustchain", l.getTranslation("N/A"));
                    vars.put("cert", l.getTranslation("N/A"));
                }
                final List<SubjectAlternateName> san = c.getSANs();
                vars.put("san", new IterableDataset() {

                    int j = 0;

                    @Override
                    public boolean next(Language l, Map<String, Object> vars) {
                        if (j == san.size()) {
                            return false;
                        }
                        vars.put("entry", san.get(j).getName() + (j < san.size() - 1 ? ", " : ""));
                        j++;
                        return true;
                    }
                });
                vars.put("login", c.isLoginEnabled());
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (GigiApiException e) {
                e.format(out, l, getDefaultVars(req));
            }
            certDisplay.output(out, getLanguage(req), vars);

            return;
        }

        HashMap<String, Object> vars = new HashMap<String, Object>();
        new CertificateModificationForm(req, req.getParameter("withRevoked") != null).output(out, getLanguage(req), vars);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        if (ac == null) {
            return false;
        }
        if (support) {
            return ac.canSupport();
        } else {
            return true;
        }
    }
}
