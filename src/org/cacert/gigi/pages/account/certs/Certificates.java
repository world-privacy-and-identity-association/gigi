package org.cacert.gigi.pages.account.certs;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.CACertificate;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.HandlesMixedRequest;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.PEM;

import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X509CRLImpl;
import sun.security.x509.X509CertImpl;

public class Certificates extends Page implements HandlesMixedRequest {

    private Template certDisplay = new Template(Certificates.class.getResource("CertificateDisplay.templ"));

    public static final String PATH = "/account/certs";

    static class TrustchainIterable implements IterableDataset {

        CACertificate cert;

        public TrustchainIterable(CACertificate cert) {
            this.cert = cert;
        }

        @Override
        public boolean next(Language l, Map<String, Object> vars) {
            if (cert == null) {
                return false;
            }
            vars.put("name", cert.getKeyname());
            vars.put("link", cert.getLink());
            if (cert.isSelfsigned()) {
                cert = null;
                return true;
            }
            cert = cert.getParent();
            return true;
        }

    }

    public Certificates() {
        super("Certificates");
    }

    @Override
    public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {

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
        } else if (pi.endsWith(".cer")) {
            cer = true;
            pi = pi.substring(0, pi.length() - 4);
        }
        String serial = pi;
        try {
            Certificate c = Certificate.getBySerial(serial);
            if (c == null || LoginPage.getAuthorizationContext(req).getTarget().getId() != c.getOwner().getId()) {
                resp.sendError(404);
                return true;
            }
            X509Certificate cert = c.cert();
            if ( !crt && !cer) {
                return false;
            }
            ServletOutputStream out = resp.getOutputStream();
            if (crt) {
                out.println(PEM.encode("CERTIFICATE", cert.getEncoded()));
                if (req.getParameter("chain") != null) {
                    CACertificate ca = c.getParent();
                    while ( !ca.isSelfsigned()) {
                        out.println(PEM.encode("CERTIFICATE", ca.getCertificate().getEncoded()));
                        ca = ca.getParent();
                    }
                    if (req.getParameter("noAnchor") == null) {
                        out.println(PEM.encode("CERTIFICATE", ca.getCertificate().getEncoded()));
                    }
                }
            } else if (cer) {
                if (req.getParameter("install") != null) {
                    PKCS7 p7 = toP7Chain(c);
                    p7.encodeSignedData(out);
                    /*
                     * ContentInfo ci = toCIChain(c); try (DerOutputStream dos =
                     * new DerOutputStream()) { ci.encode(dos);
                     * out.write(dos.toByteArray()); }
                     */
                } else {
                    out.write(cert.getEncoded());
                }
            }
        } catch (IllegalArgumentException e) {
            resp.sendError(404);
            return true;
        } catch (GeneralSecurityException e) {
            resp.sendError(404);
            return true;
        }

        return true;
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

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getQueryString() != null && !req.getQueryString().equals("") && !req.getQueryString().equals("withRevoked")) {
            return;// Block actions by get parameters.
        }
        if ( !req.getPathInfo().equals(PATH)) {
            resp.sendError(500);
            return;
        }
        Form.getForm(req, CertificateModificationForm.class).submit(resp.getWriter(), req);
        doGet(req, resp);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        String pi = req.getPathInfo().substring(PATH.length());
        if (pi.length() != 0) {
            pi = pi.substring(1);

            String serial = pi;
            Certificate c = Certificate.getBySerial(serial);
            if (c == null || LoginPage.getAuthorizationContext(req).getTarget().getId() != c.getOwner().getId()) {
                resp.sendError(404);
                return;
            }
            HashMap<String, Object> vars = new HashMap<>();
            vars.put("serial", URLEncoder.encode(serial, "UTF-8"));
            vars.put("trustchain", new TrustchainIterable(c.getParent()));
            try {
                vars.put("cert", c.cert());
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
            certDisplay.output(out, getLanguage(req), vars);

            return;
        }

        HashMap<String, Object> vars = new HashMap<String, Object>();
        new CertificateModificationForm(req, req.getParameter("withRevoked") != null).output(out, getLanguage(req), vars);
    }

}
