package club.wpia.gigi.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.CRLReason;
import java.security.cert.Extension;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import sun.security.provider.certpath.CertId;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;

public class OCSPResponse {

    private Extension nonceExt;

    public static class SingleResponse {

        private final CertId target;

        private final Date thisUpdate;

        private final Date nextUpdate;

        private final Date revoked;

        private final CRLReason res;

        private final boolean unknown;

        public SingleResponse(CertId target, Date thisUpdate, Date nextUpdate) {
            this(target, thisUpdate, nextUpdate, null);
        }

        public SingleResponse(CertId target, Date thisUpdate, Date nextUpdate, Date revoked) {
            this(target, thisUpdate, nextUpdate, revoked, null);
        }

        public SingleResponse(CertId target, Date thisUpdate, Date nextUpdate, Date revoked, CRLReason res) {
            this.target = target;
            this.thisUpdate = thisUpdate;
            this.nextUpdate = nextUpdate;
            this.revoked = revoked;
            this.res = res;
            unknown = false;
        }

        public SingleResponse(CertId target, Date thisUpdate, Date nextUpdate, boolean unkown) {
            this.target = target;
            this.thisUpdate = thisUpdate;
            this.nextUpdate = nextUpdate;
            this.revoked = null;
            this.res = null;
            this.unknown = unkown;
        }

        // @formatter:off
        // from: https://tools.ietf.org/html/rfc6960#appendix-B.1
        // SingleResponse ::= SEQUENCE {
        //     certID                  CertID,
        //     certStatus              CertStatus,
        //     thisUpdate              GeneralizedTime,
        //     nextUpdate          [0] EXPLICIT GeneralizedTime OPTIONAL,
        //     singleExtensions    [1] EXPLICIT Extensions OPTIONAL }
        // 
        //  CertStatus ::= CHOICE {
        //     good                [0] IMPLICIT NULL,
        //     revoked             [1] IMPLICIT RevokedInfo,
        //     unknown             [2] IMPLICIT UnknownInfo }
        // 
        //  RevokedInfo ::= SEQUENCE {
        //     revocationTime          GeneralizedTime,
        //     revocationReason    [0] EXPLICIT CRLReason OPTIONAL }
        // @formatter:on
        private DerValue produceSingleResponse() throws IOException {
            try (DerOutputStream r = new DerOutputStream()) {
                try (DerOutputStream target = new DerOutputStream()) {
                    this.target.encode(target);
                    if (revoked == null && !unknown) {
                        target.putTag(DerValue.TAG_CONTEXT, false, (byte) 0);
                        target.write(0);
                    } else if (revoked == null && unknown) {
                        target.putTag(DerValue.TAG_CONTEXT, false, (byte) 2);
                        target.write(0);
                    } else {
                        try (DerOutputStream gt = new DerOutputStream()) {
                            gt.putGeneralizedTime(revoked);
                            // revocationReason [0] EXPLICIT CRLReason OPTIONAL
                            if (res != null) {
                                try (DerOutputStream crlr = new DerOutputStream()) {
                                    crlr.putEnumerated(res.ordinal());
                                    gt.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0), crlr);
                                }
                            }

                            target.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 1), gt);
                        }

                    }
                    target.putGeneralizedTime(thisUpdate);
                    try (DerOutputStream gt = new DerOutputStream()) {
                        gt.putGeneralizedTime(nextUpdate);
                        target.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0), gt);
                    }

                    r.write(DerValue.tag_Sequence, target);
                }
                return new DerValue(r.toByteArray());
            }
        }
    }

    private final SingleResponse[] res;

    private X509Certificate[] signers;

    private final X500Principal dn;

    private final byte[] keyHash;

    public OCSPResponse(X500Principal dn, SingleResponse[] res) {
        this.dn = dn;
        keyHash = null;
        this.res = res;
    }

    public OCSPResponse(byte[] keyHash, SingleResponse[] res) {
        dn = null;
        this.keyHash = keyHash;
        this.res = res;
    }

    private OCSPResponse() {
        dn = null;
        res = null;
        keyHash = null;
    }

    public void setSigners(X509Certificate[] signers) {
        this.signers = signers;
    }

    /**
     * Produce possibly signed binary data for this OCSPResponse
     * 
     * @param s
     *            the signature to sign the data with. Always required for
     *            publicly visible instance.
     * @return the binary representation
     * @throws IOException
     *             if IO fails.
     * @throws GeneralSecurityException
     *             if signing fails.
     */
    // @formatter:off
    // from: https://tools.ietf.org/html/rfc6960#appendix-B.1
    // OCSPResponse ::= SEQUENCE {
    //    responseStatus          OCSPResponseStatus,
    //    responseBytes       [0] EXPLICIT ResponseBytes OPTIONAL }
    // 
    // OCSPResponseStatus ::= ENUMERATED {
    //    successful          (0),  -- Response has valid confirmations
    //    malformedRequest    (1),  -- Illegal confirmation request
    //    internalError       (2),  -- Internal error in issuer
    //    tryLater            (3),  -- Try again later
    //                              -- (4) is not used
    //    sigRequired         (5),  -- Must sign the request
    //    unauthorized        (6)   -- Request unauthorized
    // }
    // 
    // ResponseBytes ::= SEQUENCE {
    //    responseType            OBJECT IDENTIFIER,
    //    response                OCTET STRING }
    // @formatter:on
    public byte[] produceResponce(Signature s) throws IOException, GeneralSecurityException {
        try (DerOutputStream dos2 = new DerOutputStream()) {
            try (DerOutputStream dos = new DerOutputStream()) {
                if (res != null) {
                    dos.putEnumerated(0); // successful
                    ObjectIdentifier ocspBasic = new ObjectIdentifier(new int[] {
                            1, 3, 6, 1, 5, 5, 7, 48, 1, 1
                    });
                    try (DerOutputStream tagS = new DerOutputStream()) {
                        try (DerOutputStream responseBytes = new DerOutputStream()) {
                            responseBytes.putOID(ocspBasic);
                            responseBytes.putOctetString(produceBasicOCSPResponse(s));
                            tagS.write(DerValue.tag_Sequence, responseBytes);
                        }
                        dos.write((byte) 0xA0, tagS);
                    }
                } else {
                    dos.putEnumerated(1); // malformed request
                }

                dos2.write(DerValue.tag_Sequence, dos);
            }
            return dos2.toByteArray();
        }

    }

    // @formatter:off
    // from: https://tools.ietf.org/html/rfc6960#appendix-B.1
    // BasicOCSPResponse ::= SEQUENCE {
    //     tbsResponseData          ResponseData,
    //     signatureAlgorithm       AlgorithmIdentifier,
    //     signature                BIT STRING,
    //     certs                [0] EXPLICIT SEQUENCE OF Certificate OPTIONAL }
    // @formatter:on
    private byte[] produceBasicOCSPResponse(Signature s) throws IOException, GeneralSecurityException {

        try (DerOutputStream o = new DerOutputStream()) {
            try (DerOutputStream basicReponse = new DerOutputStream()) {
                produceResponseData(basicReponse);
                byte[] toSign = basicReponse.toByteArray();

                AlgorithmId.get(s.getAlgorithm()).encode(basicReponse);
                s.update(toSign);
                basicReponse.putBitString(s.sign());

                if (signers != null) {
                    try (DerOutputStream certSeq = new DerOutputStream()) {
                        try (DerOutputStream certs = new DerOutputStream()) {
                            for (X509Certificate signer : signers) {
                                certs.write(signer.getEncoded());
                            }
                            certSeq.write(DerValue.tag_Sequence, certs);
                        }
                        basicReponse.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0), certSeq);
                    }
                }

                o.write(DerValue.tag_Sequence, basicReponse.toByteArray());
            }
            return o.toByteArray();
        }

    }

    // @formatter:off
    // from: https://tools.ietf.org/html/rfc6960#appendix-B.1
    // ResponseData ::= SEQUENCE {
    //     version             [0] EXPLICIT Version DEFAULT v1,
    //     responderID             ResponderID,
    //     producedAt              GeneralizedTime,
    //     responses               SEQUENCE OF SingleResponse,
    //     responseExtensions  [1] EXPLICIT Extensions OPTIONAL }
    //  
    //  ResponderID ::= CHOICE {
    //     byName              [1] Name,
    //     byKey               [2] KeyHash }
    //
    //  KeyHash ::= OCTET STRING -- SHA-1 hash of responder's public key
    //          -- (i.e., the SHA-1 hash of the value of the
    //          -- BIT STRING subjectPublicKey [excluding
    //          -- the tag, length, and number of unused
    //          -- bits] in the responder's certificate)
    // @formatter:on
    private void produceResponseData(DerOutputStream basicReponse) throws IOException {
        try (DerOutputStream tbsResp = new DerOutputStream()) {
            produceResponderId(tbsResp);
            tbsResp.putGeneralizedTime(new Date(System.currentTimeMillis()));
            DerValue[] tgt = new DerValue[res.length];
            int i = 0;
            for (SingleResponse c : res) {
                tgt[i++] = c.produceSingleResponse();
            }
            tbsResp.putSequence(tgt);

            if (nonceExt != null) {
                try (DerOutputStream extsSeq = new DerOutputStream()) {
                    try (DerOutputStream extsOut = new DerOutputStream()) {
                        nonceExt.encode(extsOut);
                        extsSeq.write(DerValue.tag_Sequence, extsOut);
                        tbsResp.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 1), extsSeq);
                    }
                }
            }
            basicReponse.write(DerValue.tag_Sequence, tbsResp.toByteArray());
        }
    }

    private void produceResponderId(DerOutputStream tbsResp) throws IOException {
        if (dn != null) {
            tbsResp.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 1), dn.getEncoded());
        } else {
            try (DerOutputStream dos = new DerOutputStream()) {
                dos.putOctetString(keyHash);
                tbsResp.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 2), dos);
            }
            // by hash
        }
    }

    public void updateNonce(OCSPRequest or) {
        nonceExt = or.getNonceExt();
    }

    public static byte[] invalid() throws IOException, GeneralSecurityException {
        return new OCSPResponse().produceResponce(null);
    }
}
