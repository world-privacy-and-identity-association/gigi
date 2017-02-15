package club.wpia.gigi.util;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.servlet.ServletOutputStream;

import club.wpia.gigi.dbObjects.CACertificate;
import club.wpia.gigi.dbObjects.Certificate;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X509CRLImpl;
import sun.security.x509.X509CertImpl;

public class CertExporter {

    private CertExporter() {}

    public static void writeCertCrt(Certificate c, ServletOutputStream out, boolean doChain, boolean includeAnchor, boolean includeLeaf) throws IOException, GeneralSecurityException {
        X509Certificate cert = c.cert();
        if (includeLeaf) {
            out.println(PEM.encode("CERTIFICATE", cert.getEncoded()));
        }
        if (doChain) {
            CACertificate ca = c.getParent();
            while ( !ca.isSelfsigned()) {
                out.println(PEM.encode("CERTIFICATE", ca.getCertificate().getEncoded()));
                ca = ca.getParent();
            }
            if (includeAnchor) {
                out.println(PEM.encode("CERTIFICATE", ca.getCertificate().getEncoded()));
            }
        }
    }

    public static void writeCertCer(Certificate c, ServletOutputStream out, boolean doChain, boolean includeAnchor) throws IOException, GeneralSecurityException {
        X509Certificate cert = c.cert();
        if (doChain) {
            PKCS7 p7 = toP7Chain(c);
            p7.encodeSignedData(out);
        } else {
            out.write(cert.getEncoded());
        }
    }

    private static PKCS7 toP7Chain(Certificate c) throws IOException, GeneralSecurityException {
        LinkedList<X509Certificate> ll = getChain(c);
        PKCS7 p7 = new PKCS7(new AlgorithmId[0], new ContentInfo(ContentInfo.DATA_OID, null), ll.toArray(new X509Certificate[ll.size()]), new SignerInfo[0]) {

            @Override
            public void encodeSignedData(DerOutputStream out) throws IOException {
                DerOutputStream signedData = new DerOutputStream();
                BigInteger version = getVersion();
                AlgorithmId[] digestAlgorithmIds = getDigestAlgorithmIds();
                ContentInfo contentInfo = getContentInfo();
                X509Certificate[] certificates = getCertificates();
                X509CRL[] crls = getCRLs();
                SignerInfo[] signerInfos = getSignerInfos();

                // version
                signedData.putInteger(version);

                // digestAlgorithmIds
                signedData.putOrderedSetOf(DerValue.tag_Set, digestAlgorithmIds);

                // contentInfo
                contentInfo.encode(signedData);

                // certificates (optional)
                if (certificates != null && certificates.length != 0) {
                    DerOutputStream sub = new DerOutputStream();
                    // cast to X509CertImpl[] since X509CertImpl implements
                    // DerEncoder
                    X509CertImpl implCerts[] = new X509CertImpl[certificates.length];
                    for (int i = 0; i < certificates.length; i++) {
                        try {
                            sub.write(certificates[i].getEncoded());
                        } catch (CertificateEncodingException e) {
                            sub.close();
                            throw new IOException(e);
                        }
                        if (certificates[i] instanceof X509CertImpl) {
                            implCerts[i] = (X509CertImpl) certificates[i];
                        } else {
                            try {
                                byte[] encoded = certificates[i].getEncoded();
                                implCerts[i] = new X509CertImpl(encoded);
                            } catch (CertificateException ce) {
                                sub.close();
                                throw new IOException(ce);
                            }
                        }
                    }

                    // Add the certificate set (tagged with [0] IMPLICIT)
                    // to the signed data
                    signedData.write((byte) 0xA0, sub);
                    sub.close();
                }

                // CRLs (optional)
                if (crls != null && crls.length != 0) {
                    // cast to X509CRLImpl[] since X509CRLImpl implements
                    // DerEncoder
                    Set<X509CRLImpl> implCRLs = new HashSet<X509CRLImpl>(crls.length);
                    for (X509CRL crl : crls) {
                        if (crl instanceof X509CRLImpl) {
                            implCRLs.add((X509CRLImpl) crl);
                        } else {
                            try {
                                byte[] encoded = crl.getEncoded();
                                implCRLs.add(new X509CRLImpl(encoded));
                            } catch (CRLException ce) {
                                throw new IOException(ce);
                            }
                        }
                    }

                    // Add the CRL set (tagged with [1] IMPLICIT)
                    // to the signed data
                    signedData.putOrderedSetOf((byte) 0xA1, implCRLs.toArray(new X509CRLImpl[implCRLs.size()]));
                }

                // signerInfos
                signedData.putOrderedSetOf(DerValue.tag_Set, signerInfos);

                // making it a signed data block
                DerValue signedDataSeq = new DerValue(DerValue.tag_Sequence, signedData.toByteArray());

                // making it a content info sequence
                ContentInfo block = new ContentInfo(ContentInfo.SIGNED_DATA_OID, signedDataSeq);

                // writing out the contentInfo sequence
                block.encode(out);
            }

        };
        return p7;
    }

    private static LinkedList<X509Certificate> getChain(Certificate c) throws IOException, GeneralSecurityException {
        LinkedList<X509Certificate> ll = new LinkedList<>();
        ll.add(c.cert());
        CACertificate ca = c.getParent();
        while ( !ca.isSelfsigned()) {
            ll.add(ca.getCertificate());
            ca = ca.getParent();
        }
        ll.add(ca.getCertificate());
        return ll;
    }

}
