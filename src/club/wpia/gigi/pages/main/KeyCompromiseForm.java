package club.wpia.gigi.pages.main;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.util.PEM;
import club.wpia.gigi.util.RandomToken;
import club.wpia.gigi.util.RateLimit;
import club.wpia.gigi.util.RateLimit.RateLimitException;

public class KeyCompromiseForm extends Form {

    private static final Template t = new Template(KeyCompromiseForm.class.getResource("KeyCompromiseForm.templ"));

    // 50 per 5 min
    public static final RateLimit RATE_LIMIT = new RateLimit(50, 5 * 60 * 1000);

    private final String challenge;

    public static final String CHALLENGE_PREFIX = "This private key has been compromised. Challenge: ";

    public static final TranslateCommand NOT_LOADED = new TranslateCommand("Certificate could not be loaded");

    public static final TranslateCommand NOT_FOUND = new TranslateCommand("Certificate to revoke not found");

    public KeyCompromiseForm(HttpServletRequest hsr) {
        super(hsr);
        challenge = RandomToken.generateToken(32);
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        if (RATE_LIMIT.isLimitExceeded(req.getRemoteAddr())) {
            throw new RateLimitException();
        }
        Certificate c = null;
        X509Certificate cert = null;
        String serial = req.getParameter("serial");
        String certData = req.getParameter("cert");
        if (serial != null && !serial.isEmpty()) {
            c = fetchCertificate(serial);
            try {
                cert = c.cert();
            } catch (IOException e) {
                throw new PermamentFormException(new GigiApiException(NOT_LOADED));
            } catch (GeneralSecurityException e) {
                throw new PermamentFormException(new GigiApiException(NOT_LOADED));
            }
        }
        if (certData != null && !certData.isEmpty()) {
            X509Certificate c0;
            byte[] supplied;
            try {
                supplied = PEM.decode("CERTIFICATE", certData);
                c0 = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(supplied));
            } catch (IllegalArgumentException e1) {
                throw new PermamentFormException(new GigiApiException("Your certificate could not be parsed"));
            } catch (CertificateException e1) {
                throw new PermamentFormException(new GigiApiException("Your certificate could not be parsed"));
            }
            try {
                String ser = c0.getSerialNumber().toString(16);
                c = fetchCertificate(ser);
                cert = c.cert();
                if ( !Arrays.equals(supplied, cert.getEncoded())) {
                    throw new PermamentFormException(new GigiApiException(NOT_FOUND));
                }
            } catch (IOException e) {
                throw new PermamentFormException(new GigiApiException(NOT_LOADED));
            } catch (GeneralSecurityException e) {
                throw new PermamentFormException(new GigiApiException(NOT_LOADED));
            }
        }
        if (c == null) {
            throw new PermamentFormException(new GigiApiException("No certificate identification information provided"));
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
        Job j = c.revoke(challenge, Base64.getEncoder().encodeToString(signature), "");
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

    private Certificate fetchCertificate(String serial) {
        Certificate c;
        serial = serial.trim().toLowerCase();
        int idx = 0;
        while (idx < serial.length() && serial.charAt(idx) == '0') {
            idx++;
        }
        serial = serial.substring(idx);
        c = Certificate.getBySerial(serial);
        if (c == null) {
            throw new PermamentFormException(new GigiApiException(NOT_FOUND));
        }
        return c;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("challenge", challenge);
        vars.put("challengePrefix", CHALLENGE_PREFIX);
        t.output(out, l, vars);
    }

}
