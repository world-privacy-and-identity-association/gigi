package club.wpia.gigi.ocsp;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CRLReason;
import java.security.cert.X509Certificate;
import java.util.Date;

import club.wpia.gigi.crypto.OCSPRequest;
import club.wpia.gigi.crypto.OCSPResponse;
import club.wpia.gigi.crypto.OCSPResponse.SingleResponse;
import club.wpia.gigi.dbObjects.CACertificate;
import club.wpia.gigi.dbObjects.Certificate;
import sun.security.provider.certpath.CertId;

/**
 * An instance that creates OCSP responses.
 */
public class OCSPIssuer {

    /**
     * The CA certificate to issue OCSP responses for.
     */
    private final X509Certificate target;

    /**
     * The OCSP certificate for which we have the private key.
     */
    private final X509Certificate cert;

    /**
     * The OCSP certificate's private key to sign the responses with.
     */
    private final PrivateKey key;

    private final byte[] subjectKeyIdentifier;

    public OCSPIssuer(X509Certificate target, X509Certificate x, PrivateKey key) throws IOException, GeneralSecurityException {
        this.target = target;
        this.cert = x;
        this.key = key;
        this.subjectKeyIdentifier = OCSPResponder.calcKeyHash(cert, MessageDigest.getInstance("SHA-1"));
    }

    public X509Certificate getTarget() {
        return target;
    }

    public byte[] getKeyId() {
        return subjectKeyIdentifier;
    }

    private SingleResponse respond(CertId id, Certificate cert) {
        if (cert != null) {
            Date dt = cert.getRevocationDate();
            if (dt != null) {
                return new OCSPResponse.SingleResponse(id, new Date(System.currentTimeMillis() - 10000), new Date(System.currentTimeMillis() + 10000), dt, CRLReason.UNSPECIFIED);
            } else {
                return new OCSPResponse.SingleResponse(id, new Date(System.currentTimeMillis() - 10000), new Date(System.currentTimeMillis() + 10000));
            }
        } else {
            return new OCSPResponse.SingleResponse(id, new Date(System.currentTimeMillis() - 10000), new Date(System.currentTimeMillis() + 10000), true);
        }
    }

    /**
     * Responds with the status of one certificate.
     * 
     * @param req
     *            the {@link OCSPRequest} to take the nonce from.
     * @param id
     *            The certificate for which to look up revocation information.
     * @return the signed {@link OCSPResponse} in binary data.
     * @throws GeneralSecurityException
     *             if signing fails
     * @throws IOException
     *             if encoding fails
     */
    public byte[] respondBytes(OCSPRequest req, CertId id) throws GeneralSecurityException, IOException {
        Certificate tcert = Certificate.getBySerial(id.getSerialNumber());
        if (tcert == null) {
            return OCSPResponse.invalid();
        }
        CACertificate cc = tcert.getParent();
        if ( !cc.getCertificate().getSubjectDN().equals(getTarget().getSubjectDN())) {
            tcert = null;
            OCSPResponder.log.warning("OCSP request with different Issuer: Based on serial: " + cc.getCertificate().getSubjectDN() + " but based on request: " + getTarget().getSubjectDN());
            return OCSPResponse.invalid();
        }

        SingleResponse[] responses = new OCSPResponse.SingleResponse[1];
        responses[0] = respond(id, tcert);

        OCSPResponse ocspResponse = new OCSPResponse(getKeyId(), responses);
        if (cert != getTarget()) {
            ocspResponse.setSigners(new X509Certificate[] {
                    cert
            });
        } else {
            ocspResponse.setSigners(new X509Certificate[] {
                    // getCert()
            });

        }
        ocspResponse.updateNonce(req);
        Signature s = Signature.getInstance("SHA512WithRSA");
        s.initSign(key);
        return ocspResponse.produceResponce(s);
    }
}
