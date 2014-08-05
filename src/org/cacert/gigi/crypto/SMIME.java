package org.cacert.gigi.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Random;

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

        mimeEncode(contents, Base64.getEncoder().encodeToString(bOut.toByteArray()).replaceAll("(.{64})(?=.)", "$1\n"), to);
    }

    static Random r = new Random();

    private static void mimeEncode(String contents, String signature, PrintWriter to) {
        String boundary = generateBoundary(contents, null);
        to.println("MIME-Version: 1.0");
        to.println("Content-Type: multipart/signed; protocol=\"application/x-pkcs7-signature\"; micalg=\"sha1\"; boundary=\"" + boundary + "\"");
        to.println("");
        to.println("This is an S/MIME signed message");
        to.println("");
        to.println("--" + boundary);
        to.println(contents);
        to.println("--" + boundary);
        to.println("Content-Type: application/x-pkcs7-signature; name=\"smime.p7s\"");
        to.println("Content-Transfer-Encoding: base64");
        to.println("Content-Disposition: attachment; filename=\"smime.p7s\"");
        to.println("");
        to.println(signature);
        to.println();
        to.println("--" + boundary + "--");
    }

    private static String generateBoundary(String contents, String contents2) {
        String boundary = "";
        while (contents.contains(boundary) || (contents2 != null && contents2.contains(boundary))) {
            boundary = "--" + new BigInteger(16 * 8, r).toString(16).toUpperCase();
        }
        return boundary;
    }
}
