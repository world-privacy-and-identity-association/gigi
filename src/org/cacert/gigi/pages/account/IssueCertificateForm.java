package org.cacert.gigi.pages.account;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Certificate;
import org.cacert.gigi.Digest;
import org.cacert.gigi.EmailAddress;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.Certificate.CSRType;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.HashAlgorithms;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.RandomToken;

import sun.security.pkcs10.PKCS10;

/**
 * This class represents a form that is used for issuing certificates. This
 * class uses "sun.security" and therefore needs "-XDignore.symbol.file"
 */
public class IssueCertificateForm extends Form {

    User u;

    Digest selectedDigest = Digest.getDefault();

    boolean login;

    String csr;

    private final static Template t = new Template(IssueCertificateForm.class.getResource("IssueCertificateForm.templ"));

    private final static Template tIni = new Template(MailCertificateAdd.class.getResource("RequestCertificate.templ"));

    String spkacChallange;

    public IssueCertificateForm(HttpServletRequest hsr) {
        super(hsr);
        u = LoginPage.getUser(hsr);
        spkacChallange = RandomToken.generateToken(16);
    }

    Certificate result;

    private CSRType csrType;

    public Certificate getResult() {
        return result;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        String csr = req.getParameter("CSR");
        String spkac = req.getParameter("SPKAC");
        try {
            if (csr != null) {
                PKCS10 parsed = parseCSR(csr);
                out.println(parsed.getSubjectName().getCommonName());
                out.println(parsed.getSubjectName().getCountry());
                out.println("CSR DN: " + parsed.getSubjectName() + "<br/>");
                PublicKey pk = parsed.getSubjectPublicKeyInfo();
                out.println("Type: " + pk.getAlgorithm() + "<br/>");
                if (pk instanceof RSAPublicKey) {
                    out.println("Exponent: " + ((RSAPublicKey) pk).getPublicExponent() + "<br/>");
                    out.println("Length: " + ((RSAPublicKey) pk).getModulus().bitLength());
                } else if (pk instanceof DSAPublicKey) {
                    DSAPublicKey dpk = (DSAPublicKey) pk;
                    out.println("Length: " + dpk.getY().bitLength() + "<br/>");
                    out.println(dpk.getParams());
                } else if (pk instanceof ECPublicKey) {
                    ECPublicKey epk = (ECPublicKey) pk;
                    out.println("Length-x: " + epk.getW().getAffineX().bitLength() + "<br/>");
                    out.println("Length-y: " + epk.getW().getAffineY().bitLength() + "<br/>");
                    out.println(epk.getParams().getCurve());
                }
                out.println("<br/>digest: sha256<br/>");
                this.csr = csr;
                this.csrType = CSRType.CSR;
            } else if (spkac != null) {
                String cleanedSPKAC = "SPKAC=" + spkac.replaceAll("[\r\n]", "");
                try {
                    checkSPKAC(cleanedSPKAC, spkacChallange);
                    this.csr = cleanedSPKAC;
                    this.csrType = CSRType.SPKAC;
                } catch (GigiApiException e) {
                    e.format(out, Page.getLanguage(req));
                }

            } else {
                login = "1".equals(req.getParameter("login"));
                String hashAlg = req.getParameter("hash_alg");
                if (hashAlg != null) {
                    selectedDigest = Digest.valueOf(hashAlg);
                }
                if (req.getParameter("CCA") == null) {
                    outputError(out, req, "You need to accept the CCA.");
                    return false;
                }
                System.out.println("issuing " + selectedDigest);
                result = new Certificate(LoginPage.getUser(req).getId(), "/commonName=CAcert WoT User", selectedDigest.toString(), this.csr, this.csrType);
                try {
                    result.issue().waitFor(60000);
                    return true;
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void checkSPKAC(String csr, String spkacChallange) throws IOException, GigiApiException {
        Process p = Runtime.getRuntime().exec(new String[] {
                "openssl", "spkac", "-verify"
        });
        OutputStream outputStream = p.getOutputStream();
        outputStream.write(csr.getBytes());
        outputStream.flush();
        outputStream.close();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
        String line;
        String challenge = null;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            String challengePrefix = "Challenge String: ";
            if (line.startsWith(challengePrefix)) {
                challenge = line.substring(challengePrefix.length());
            }
        }
        GigiApiException gae = new GigiApiException();
        if ( !spkacChallange.equals(challenge)) {
            gae.mergeInto(new GigiApiException("The challenge-response code of your certificate request did not match. Can't continue with certificaterequest."));
        }
        try {
            if (p.waitFor() != 0) {
                gae.mergeInto(new GigiApiException("The signature of your certificate request is invalid. Can't continue with certificaterequest."));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if ( !gae.isEmpty()) {
            throw gae;
        }
    }

    private PKCS10 parseCSR(String csr) throws IOException, GeneralSecurityException {
        csr = csr.replaceFirst("-----BEGIN (NEW )?CERTIFICATE REQUEST-----", "");
        csr = csr.replaceFirst("-----END (NEW )?CERTIFICATE REQUEST-----", "");
        csr = csr.replace("\r", "");
        csr = csr.replace("\n", "");
        byte[] b = Base64.getDecoder().decode(csr);
        // Also checks signature validity
        return new PKCS10(b);
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        if (csr == null) {
            HashMap<String, Object> vars2 = new HashMap<String, Object>(vars);
            vars2.put("csrf", getCSRFToken());
            vars2.put("csrf_name", getCsrfFieldName());
            vars2.put("spkacChallange", spkacChallange);
            tIni.output(out, l, vars2);
            return;
        } else {
            super.output(out, l, vars);
        }
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        HashMap<String, Object> vars2 = new HashMap<String, Object>(vars);
        vars2.put("CCA", "<a href='/policy/CAcertCommunityAgreement.html'>CCA</a>");

        final EmailAddress[] ea = u.getEmails();
        vars2.put("emails", new IterableDataset() {

            int count;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (count >= ea.length) {
                    return false;
                }
                vars.put("id", ea[count].getId());
                vars.put("value", ea[count].getAddress());
                count++;
                return true;
            }
        });
        vars2.put("hashs", new HashAlgorithms(selectedDigest));
        t.output(out, l, vars2);
    }
}
