package club.wpia.gigi.pages.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.MailTemplate;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.util.PEM;
import club.wpia.gigi.util.RandomToken;
import club.wpia.gigi.util.RateLimit;
import club.wpia.gigi.util.RateLimit.RateLimitException;
import club.wpia.gigi.util.ServerConstants;

public class KeyCompromiseForm extends Form {

    public static final String CONFIDENTIAL_MARKER = "*CONFIDENTIAL*";

    private static final Template t = new Template(KeyCompromiseForm.class.getResource("KeyCompromiseForm.templ"));

    // 50 per 5 min
    public static final RateLimit RATE_LIMIT = new RateLimit(50, 5 * 60 * 1000);

    private final String challenge;

    public static final String CHALLENGE_PREFIX = "This private key has been compromised. Challenge: ";

    public static final TranslateCommand NOT_FOUND = new TranslateCommand("Certificate to revoke not found");

    private static final MailTemplate revocationNotice = new MailTemplate(KeyCompromiseForm.class.getResource("RevocationNotice.templ"));

    public KeyCompromiseForm(HttpServletRequest hsr) {
        super(hsr);
        challenge = RandomToken.generateToken(32);
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        if (RATE_LIMIT.isLimitExceeded(req.getRemoteAddr())) {
            throw new RateLimitException();
        }
        Certificate c;
        try {
            c = Certificate.locateCertificate(req.getParameter("serial"), req.getParameter("cert"));
            if (c == null) {
                throw new GigiApiException(NOT_FOUND);
            }
        } catch (GigiApiException e) {
            throw new PermamentFormException(e);
        }

        X509Certificate cert;
        try {
            cert = c.cert();
        } catch (IOException | GeneralSecurityException e) {
            throw new PermamentFormException(new GigiApiException(Certificate.NOT_LOADED));
        }

        if (c.getStatus() == CertificateStatus.REVOKED) {
            return new SuccessMessageResult(new TranslateCommand("Certificate had already been revoked"));
        }
        String inSig = req.getParameter("signature");
        byte[] signature = null;
        if (inSig != null && !inSig.isEmpty()) {
            try {
                signature = Base64.getDecoder().decode(inSig);
            } catch (IllegalArgumentException e) {
                throw new PermamentFormException(new GigiApiException("Signature is malformed"));
            }
        }
        String priv = req.getParameter("priv");
        if (signature == null && priv != null && !priv.isEmpty()) {
            try {
                PKCS8EncodedKeySpec k = new PKCS8EncodedKeySpec(PEM.decode("PRIVATE KEY", priv));
                RSAPrivateKey pk = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(k);
                signature = sign(pk, challenge);
            } catch (IllegalArgumentException e) {
                throw new PermamentFormException(new GigiApiException("Private Key is malformed"));
            } catch (GeneralSecurityException e) {
                throw new PermamentFormException(new GigiApiException("Private Key is malformed"));
            }
        }
        if (signature == null) {
            throw new PermamentFormException(new GigiApiException("No verification provided."));
        }

        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(cert.getPublicKey());
            sig.update(CHALLENGE_PREFIX.getBytes("UTF-8"));
            sig.update(challenge.getBytes("UTF-8"));
            if ( !sig.verify(signature)) {
                throw new PermamentFormException(new GigiApiException("Verification does not match."));
            }
        } catch (GeneralSecurityException e) {
            throw new PermamentFormException(new GigiApiException("Wasn't able to generate signature."));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String message = req.getParameter("message");
        if (message != null && message.isEmpty()) {
            message = null;
        }
        if (message != null) {
            if (message.startsWith(CONFIDENTIAL_MARKER)) {
                message = " " + message;
            }
            String confidential = req.getParameter("confidential");
            if (confidential != null && !confidential.isEmpty()) {
                message = CONFIDENTIAL_MARKER + "\r\n" + message;
            }
            if (message.contains("---")) {
                throw new GigiApiException("Your message may not contain '---'.");
            }
            // convert all line endings to CRLF
            message = message.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\r\n");
            if ( !message.matches("[ -~\r\n\t]*")) {
                throw new GigiApiException("Your message may only contain printable ASCII characters, tab, newline and space.");
            }
        }
        CertificateOwner co = c.getOwner();
        String primaryEmail;
        Language l = Language.getInstance(Locale.ENGLISH);
        if (co instanceof User) {
            primaryEmail = ((User) co).getEmail();
            l = Language.getInstance(((User) co).getPreferredLocale());
        } else if (co instanceof Organisation) {
            primaryEmail = ((Organisation) co).getContactEmail();
        } else {
            throw new IllegalArgumentException("certificate owner of unknown type");
        }
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("appName", ServerConstants.getAppName());
        if (message != null && !message.startsWith(CONFIDENTIAL_MARKER)) {
            vars.put("message", message);
        } else {
            vars.put("message", null);
        }
        vars.put("serial", c.getSerial());
        try {
            revocationNotice.sendMail(l, vars, primaryEmail);
        } catch (IOException e) {
            throw new GigiApiException("Sending the notification mail failed.");
        }
        Job j = c.revoke(challenge, Base64.getEncoder().encodeToString(signature), message);
        if ( !j.waitFor(60000)) {
            throw new PermamentFormException(new GigiApiException("Revocation timed out."));
        }
        if (c.getStatus() != CertificateStatus.REVOKED) {
            throw new PermamentFormException(new GigiApiException("Revocation failed."));
        }
        return new SuccessMessageResult(new TranslateCommand("Certificate is revoked."));
    }

    public static byte[] sign(PrivateKey pk, String challenge) throws GeneralSecurityException {
        byte[] signature;
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(pk);
        try {
            sig.update(CHALLENGE_PREFIX.getBytes("UTF-8"));
            sig.update(challenge.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        signature = sig.sign();
        return signature;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("challenge", challenge);
        vars.put("challengePrefix", CHALLENGE_PREFIX);
        t.output(out, l, vars);
    }

}
