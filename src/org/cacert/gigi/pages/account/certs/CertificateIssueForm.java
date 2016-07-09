package org.cacert.gigi.pages.account.certs;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.SubjectAlternateName;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.CertificateValiditySelector;
import org.cacert.gigi.output.HashAlgorithms;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;
import org.cacert.gigi.util.RandomToken;

/**
 * This class represents a form that is used for issuing certificates. This
 * class uses "sun.security" and therefore needs "-XDignore.symbol.file"
 */
public class CertificateIssueForm extends Form {

    private final static Template t = new Template(CertificateIssueForm.class.getResource("CertificateIssueForm.templ"));

    private final static Template tIni = new Template(CertificateAdd.class.getResource("RequestCertificate.templ"));

    private AuthorizationContext c;

    private String spkacChallenge;

    private boolean login;

    public CertificateIssueForm(HttpServletRequest hsr) {
        super(hsr);
        c = LoginPage.getAuthorizationContext(hsr);
        spkacChallenge = RandomToken.generateToken(16);
    }

    private Certificate result;

    public Certificate getResult() {
        return result;
    }

    private CertificateRequest cr;

    CertificateValiditySelector issueDate = new CertificateValiditySelector();

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        String csr = req.getParameter("CSR");
        String spkac = req.getParameter("SPKAC");
        try {
            try {
                if (csr != null) {
                    cr = new CertificateRequest(c, csr);
                    cr.checkKeyStrength(out);
                } else if (spkac != null) {
                    cr = new CertificateRequest(c, spkac, spkacChallenge);
                    cr.checkKeyStrength(out);
                } else if (cr != null) {
                    login = "1".equals(req.getParameter("login"));
                    issueDate.update(req);
                    GigiApiException error = new GigiApiException();

                    try {
                        cr.update(req.getParameter("CN"), req.getParameter("hash_alg"), req.getParameter("profile"), //
                                req.getParameter("org"), req.getParameter("OU"), req.getParameter("SANs"), out, req);
                    } catch (GigiApiException e) {
                        error.mergeInto(e);
                    }

                    Certificate result = null;
                    try {
                        result = cr.draft();
                    } catch (GigiApiException e) {
                        error.mergeInto(e);
                    }
                    if ( !error.isEmpty() || result == null) {
                        error.format(out, Page.getLanguage(req));
                        return false;
                    }
                    result.issue(issueDate.getFrom(), issueDate.getTo(), c.getActor()).waitFor(60000);
                    this.result = result;
                    return true;
                } else {
                    throw new GigiApiException("Error no action.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                throw new GigiApiException("Certificate Request format is invalid.");
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                throw new GigiApiException("Certificate Request format is invalid.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
        }
        return false;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        if (cr == null) {
            HashMap<String, Object> vars2 = new HashMap<String, Object>(vars);
            vars2.put("csrf", getCSRFToken());
            vars2.put("csrf_name", getCsrfFieldName());
            vars2.put("spkacChallenge", spkacChallenge);
            tIni.output(out, l, vars2);
            return;
        } else {
            super.output(out, l, vars);
        }
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        HashMap<String, Object> vars2 = new HashMap<String, Object>(vars);

        StringBuffer content = new StringBuffer();
        for (SubjectAlternateName SAN : cr.getSANs()) {
            content.append(SAN.getType().toString().toLowerCase());
            content.append(':');
            content.append(SAN.getName());
            content.append('\n');
        }

        vars2.put("CN", cr.getName());
        if (c.getTarget() instanceof Organisation) {
            vars2.put("orga", "true");
            vars2.put("department", cr.getOu());
        }
        vars2.put("validity", issueDate);
        vars2.put("emails", content.toString());
        vars2.put("hashs", new HashAlgorithms(cr.getSelectedDigest()));
        vars2.put("profiles", new IterableDataset() {

            CertificateProfile[] cps = CertificateProfile.getAll();

            int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                CertificateProfile cp;
                do {
                    if (i >= cps.length) {
                        return false;
                    }
                    cp = cps[i];
                    i++;
                } while ( !cp.canBeIssuedBy(c.getTarget(), c.getActor()));

                if (cp.getId() == cr.getProfile().getId()) {
                    vars.put("selected", " selected");
                } else {
                    vars.put("selected", "");
                }
                vars.put("key", cp.getKeyName());
                vars.put("name", cp.getVisibleName());
                return true;
            }
        });

        t.output(out, l, vars2);
    }
}
