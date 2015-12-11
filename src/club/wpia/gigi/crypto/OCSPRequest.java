package club.wpia.gigi.crypto;

import java.io.IOException;
import java.security.cert.Extension;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import sun.security.provider.certpath.CertId;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

/**
 * Adapted from {@link sun.security.provider.certpath.OCSPRequest}
 */
public class OCSPRequest {

    static final ObjectIdentifier NONCE_EXTENSION_OID = ObjectIdentifier.newInternal(new int[] {
            1, 3, 6, 1, 5, 5, 7, 48, 1, 2
    });

    // List of request CertIds
    private final List<CertId> certIds;

    private final List<Extension> extensions;

    private byte[] nonce;

    private Extension nonceExt;

    /*
     * Constructs an OCSPRequest. This constructor is used to construct an
     * unsigned OCSP Request for a single user cert.
     */
    OCSPRequest(CertId certId) {
        this(Collections.singletonList(certId));
    }

    OCSPRequest(List<CertId> certIds) {
        this.certIds = certIds;
        this.extensions = Collections.<Extension>emptyList();
    }

    OCSPRequest(List<CertId> certIds, List<Extension> extensions) {
        this.certIds = certIds;
        this.extensions = extensions;
    }

    /**
     * Creates a new OCSPRequest from its binary data.
     * 
     * @param in
     *            the binary form of the OCSP request.
     * @throws IOException
     *             if the input is malformed
     */
    public OCSPRequest(byte[] in) throws IOException {
        DerInputStream dis = new DerInputStream(in);
        DerInputStream req = dis.getDerValue().getData();
        DerInputStream tbsreq = req.getDerValue().getData();
        // req.getDerValue()optional signature

        LinkedList<Extension> exts = new LinkedList<>();
        LinkedList<CertId> cis = new LinkedList<>();
        // handles the content of structure
        // @formatter:off
        //TBSRequest ::= SEQUENCE {
        //    version             [0] EXPLICIT Version DEFAULT v1,
        //    requestorName       [1] EXPLICIT GeneralName OPTIONAL,
        //    requestList             SEQUENCE OF Request,
        //    requestExtensions   [2] EXPLICIT Extensions OPTIONAL }
        // @formatter:on
        while (tbsreq.available() > 0) {
            // Handle content
            if (tbsreq.peekByte() == DerValue.tag_Sequence) {
                for (DerValue certId : tbsreq.getSequence(1)) {
                    CertId ci = new CertId(certId.getData().getDerValue().getData());
                    cis.add(ci);
                }
                // Handle extensions
            } else if (tbsreq.peekByte() == DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 2)) {
                DerValue[] seq = tbsreq.getDerValue().getData().getSequence(5);
                for (DerValue derValue : seq) {
                    sun.security.x509.Extension e = new sun.security.x509.Extension(derValue);
                    if (e.getExtensionId().equals((Object) NONCE_EXTENSION_OID)) {
                        nonce = e.getValue();
                        nonceExt = e;
                    } else if (e.isCritical()) {
                        throw new IOException("Unknown critical extension");
                    }

                    exts.add(e);
                }
                // Skip any other element
            } else {
                tbsreq.getDerValue();
            }
        }

        if (exts.isEmpty()) {
            extensions = null;
        } else {
            extensions = Collections.unmodifiableList(exts);
        }
        certIds = Collections.unmodifiableList(cis);
    }

    byte[] encodeBytes() throws IOException {

        // encode tbsRequest
        DerOutputStream tmp = new DerOutputStream();
        DerOutputStream requestsOut = new DerOutputStream();
        for (CertId certId : certIds) {
            DerOutputStream certIdOut = new DerOutputStream();
            certId.encode(certIdOut);
            requestsOut.write(DerValue.tag_Sequence, certIdOut);
        }

        tmp.write(DerValue.tag_Sequence, requestsOut);
        if ( !extensions.isEmpty()) {
            DerOutputStream extOut = new DerOutputStream();
            for (Extension ext : extensions) {
                ext.encode(extOut);
                if (ext.getId().equals(NONCE_EXTENSION_OID.toString())) {
                    nonce = ext.getValue();
                    nonceExt = ext;
                }
            }
            DerOutputStream extsOut = new DerOutputStream();
            extsOut.write(DerValue.tag_Sequence, extOut);
            tmp.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 2), extsOut);
        }

        DerOutputStream tbsRequest = new DerOutputStream();
        tbsRequest.write(DerValue.tag_Sequence, tmp);

        // OCSPRequest without the signature
        DerOutputStream ocspRequest = new DerOutputStream();
        ocspRequest.write(DerValue.tag_Sequence, tbsRequest);

        byte[] bytes = ocspRequest.toByteArray();

        return bytes;
    }

    public List<CertId> getCertIds() {
        return certIds;
    }

    byte[] getNonce() {
        return nonce;
    }

    public Extension getNonceExt() {
        return nonceExt;
    }
}
