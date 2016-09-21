package org.cacert.gigi.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Random;

import org.cacert.gigi.util.PEM;

import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.util.DerOutputStream;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

public class SMIME {

    public static String doAlternatives(String plain, String html) {

        plain = "Content-type: text/plain\r\n\r\n" + plain;
        html = "Content-type: text/html\r\n\r\n" + html;
        String boundary = generateBoundary(plain, html);
        StringBuffer content = new StringBuffer("Content-Type: multipart/alternative; boundary=\"");
        content.append(boundary);
        content.append("\"\r\n\r\n");
        content.append("--");
        content.append(boundary);
        content.append("\r\n");
        content.append(plain);
        content.append("\r\n--");
        content.append(boundary);
        content.append("\r\n");
        content.append(html);
        content.append("\r\n--");
        content.append(boundary);
        content.append("--\r\n");
        return content.toString();

    }

    public static void smime(String contents, PrivateKey pKey, X509Certificate c, PrintWriter to) throws IOException, GeneralSecurityException {
        contents = normalizeNewlinesToCRLF(contents);

        Signature signature = Signature.getInstance("SHA1WithRSA");
        signature.initSign(pKey);
        signature.update(contents.getBytes("UTF-8"));
        byte[] signedData = signature.sign();

        // "IssuerAndSerialNumber"
        X500Name xName = X500Name.asX500Name(c.getIssuerX500Principal());
        BigInteger serial = c.getSerialNumber();

        SignerInfo sInfo = new SignerInfo(xName, serial, new AlgorithmId(AlgorithmId.SHA_oid), null, new AlgorithmId(AlgorithmId.RSAEncryption_oid), signedData, null);

        // Content is outside so content here is null.
        ContentInfo cInfo = new ContentInfo(ContentInfo.DATA_OID, null);

        // Create PKCS7 Signed data
        PKCS7 p7 = new PKCS7(new AlgorithmId[] {
                new AlgorithmId(AlgorithmId.SHA_oid)
        }, cInfo, new java.security.cert.X509Certificate[] {
                c
        }, new SignerInfo[] {
                sInfo
        });

        ByteArrayOutputStream bOut = new DerOutputStream();
        p7.encodeSignedData(bOut);

        mimeEncode(contents, PEM.formatBase64(bOut.toByteArray()), to);
    }

    private static String normalizeNewlinesToCRLF(String contents) {
        return contents.replace("\r\n", "\r").replace("\r", "\n").replace("\n", "\r\n");
    }

    private static Random r = new Random();

    private static void mimeEncode(String contents, String signature, PrintWriter to) {
        String boundary = generateBoundary(contents, null);
        to.print("MIME-Version: 1.0\r\n");
        to.print("Content-Type: multipart/signed; protocol=\"application/x-pkcs7-signature\"; micalg=\"sha1\"; boundary=\"" + boundary + "\"\r\n");
        to.print("\r\n");
        to.print("This is an S/MIME signed message\r\n");
        to.print("\r\n");
        to.print("--" + boundary + "\r\n");
        to.print(contents + "\r\n");
        to.print("--" + boundary + "\r\n");
        to.print("Content-Type: application/x-pkcs7-signature; name=\"smime.p7s\"\r\n");
        to.print("Content-Transfer-Encoding: base64\r\n");
        to.print("Content-Disposition: attachment; filename=\"smime.p7s\"\r\n");
        to.print("\r\n");
        to.print(signature + "\r\n");
        to.print("\r\n");
        to.print("--" + boundary + "--\r\n");
    }

    private static String generateBoundary(String contents, String contents2) {
        String boundary = "";
        while (contents.contains(boundary) || (contents2 != null && contents2.contains(boundary))) {
            boundary = "--" + new BigInteger(16 * 8, r).toString(16).toUpperCase();
        }
        return boundary;
    }
}
