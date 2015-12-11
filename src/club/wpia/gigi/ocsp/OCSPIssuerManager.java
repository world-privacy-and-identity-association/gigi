package club.wpia.gigi.ocsp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import club.wpia.gigi.util.PEM;
import sun.security.pkcs10.PKCS10;
import sun.security.pkcs10.PKCS10Attributes;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

/**
 * Manages the set of {@link OCSPIssuer}s by updating their OCSP certificates
 * and renewing the issuers. The Thread executing all the management work has to
 * be started manually. The {@link #get(AlgorithmId)} method provides a
 * requested set of issuers.
 */
public class OCSPIssuerManager implements Runnable {

    private final Map<String, KeyPair> openRequests = new HashMap<>();

    private Map<AlgorithmId, Map<OCSPIssuerId, OCSPIssuer>> map = new HashMap<>();

    private long nextHousekeeping;

    private boolean isGood(PrivateKey k, Certificate target, Certificate parent) throws GeneralSecurityException {
        X509Certificate ocsp = (X509Certificate) target;
        if ( !ocsp.getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.9")) {
            OCSPResponder.log.severe("OCSP cert does not have correct EKU set.");
            return false;
        }
        target.verify(parent.getPublicKey());
        if ( !matches(k, target.getPublicKey())) {
            OCSPResponder.log.severe("Public key contained in cert does not match.");
            return false;
        }
        return true;
    }

    private boolean matches(PrivateKey k, PublicKey pk) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        SecureRandom ref = new SecureRandom();
        Signature s = Signature.getInstance("SHA512WithRSA");
        s.initSign(k);
        byte[] data = new byte[20];
        ref.nextBytes(data);
        s.update(data);
        byte[] signature = s.sign();
        s.initVerify(pk);
        s.update(data);
        boolean verify = s.verify(signature);
        return verify;
    }

    private void index(AlgorithmId aid, MessageDigest md, Map<String, OCSPIssuer> toServe, Map<AlgorithmId, Map<OCSPIssuerId, OCSPIssuer>> map) {
        OCSPResponder.log.info("Indexing OCSP issuers for " + md);
        HashMap<OCSPIssuerId, OCSPIssuer> issuers = new HashMap<>();
        for (OCSPIssuer i : toServe.values()) {
            issuers.put(new OCSPIssuerId(aid, md, i.getTarget()), i);
        }
        map.put(aid, Collections.unmodifiableMap(issuers));
    }

    /**
     * Scans for CAs to issue OCSP responses for in the directory f.
     * 
     * @param f
     *            The directory to scan recursively.
     * @param keys
     *            a keystore with all private keys for all OCSP certificates
     *            (will be used and updated with new ocsp certs)
     * @param toServe
     *            A map with {@link OCSPIssuer}s to be populated with all
     *            scanned CAs
     */
    private void scanAndUpdateCAs(File f, KeyStore keys, Map<String, OCSPIssuer> toServe) {
        if (f.isDirectory()) {
            for (File f1 : f.listFiles()) {
                scanAndUpdateCAs(f1, keys, toServe);
            }
            return;
        }
        if ( !f.getName().equals("ca.crt")) {
            return;
        }
        try {
            String keyName = f.getParentFile().getName();
            OCSPResponder.log.info("CA: " + keyName);
            updateCA(f, keyName, keys, toServe);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void updateCA(File f, String keyName, KeyStore keys, Map<String, OCSPIssuer> toServe) throws GeneralSecurityException, IOException {
        X509Certificate parent = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new FileInputStream(f));

        Certificate[] storedCertificateChain = keys.getCertificateChain(keyName);

        if (storedCertificateChain == null) {
            OCSPResponder.log.info("Keystore entry for OCSP certificate for CA " + keyName + " was not found");
        } else {
            if ( !storedCertificateChain[1].equals(parent)) {
                OCSPResponder.log.severe("unexpeced CA certificate in keystore entry for OCSP certificate.");
                return;
            }
            PrivateKey key = (PrivateKey) keys.getKey(keyName, "pass".toCharArray());
            isGood(key, storedCertificateChain[0], storedCertificateChain[1]);
            toServe.put(keyName, new OCSPIssuer((X509Certificate) storedCertificateChain[1], (X509Certificate) storedCertificateChain[0], key));
        }
        boolean hasKeyRequest = openRequests.containsKey(keyName);
        File ocspCsr = new File(f.getParentFile(), "ocsp.csr");
        File ocspCrt = new File(f.getParentFile(), "ocsp.crt");
        if (hasKeyRequest) {
            KeyPair r = openRequests.get(keyName);
            if (ocspCrt.exists()) {
                X509Certificate x = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new FileInputStream(ocspCrt));
                // attempt to load ocspCrt, try if it matches with the
                // given key.
                // if it does match: load crt with key into primary
                // keystore entry. Update Issuer
                if (r.getPublic().equals(x.getPublicKey()) && isGood(r.getPrivate(), x, parent)) {
                    OCSPResponder.log.info("Loading OCSP Certificate");
                    keys.setKeyEntry(keyName, r.getPrivate(), "pass".toCharArray(), new Certificate[] {
                            x, parent
                    });
                    openRequests.remove(keyName);
                    ocspCsr.delete();
                    ocspCrt.delete();
                    toServe.put(keyName, new OCSPIssuer(parent, x, r.getPrivate()));
                } else {
                    // if it does not match: check CSR
                    OCSPResponder.log.severe("OCSP certificate does not fit.");
                }
            } else {
                // Seems the signer should work now.. Let's wait for
                // now.
            }
        } else {
            if (keys.containsAlias(keyName)) {
                X509Certificate c = (X509Certificate) keys.getCertificate(keyName);
                Date expiery = c.getNotAfter();
                Date now = new Date();
                long deltas = expiery.getTime() - now.getTime();
                deltas /= 1000;
                deltas /= 60 * 60 * 24;
                OCSPResponder.log.info("Remaining days for OCSP certificate: " + deltas);
                if (deltas > 30 * 3) {
                    return;
                }
            }
            OCSPResponder.log.info("Requesting OCSP certificate");
            // request a new OCSP certificate with a new RSA-key.
            requestNewOCSPCert(keyName, ocspCsr, ocspCrt);

            // assuming this cert will be ready in 30 seconds
            nextHousekeeping = Math.min(nextHousekeeping, System.currentTimeMillis() + 30 * 1000);
        }
    }

    private void requestNewOCSPCert(String keyName, File ocspCsr, File ocspCrt) throws IOException, GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(4096);
        KeyPair kp = kpg.generateKeyPair();
        openRequests.put(keyName, kp);
        PKCS10 p10 = new PKCS10(kp.getPublic(), new PKCS10Attributes());
        Signature s = Signature.getInstance("SHA512WithRSA");
        s.initSign(kp.getPrivate());
        p10.encodeAndSign(new X500Name("CN=OSCP Responder"), s);
        ocspCsr.delete();
        ocspCrt.delete();
        String csr = PEM.encode("CERTIFICATE REQUEST", p10.getEncoded());
        try (Writer w = new OutputStreamWriter(new FileOutputStream(ocspCsr), "UTF-8")) {
            w.write(csr);
        }
    }

    @Override
    public void run() {
        File ks = new File("ocsp.pkcs12");
        File f = new File("ocsp");
        if ( !ks.exists() || !ks.isFile() || !f.exists() || !f.isDirectory()) {
            OCSPResponder.log.info("OCSP issuing is not configured");
            return;
        }
        while (true) {
            try {
                // Hourly is enough
                nextHousekeeping = System.currentTimeMillis() + 60 * 60 * 1000;
                KeyStore keys;

                try {
                    keys = KeyStore.getInstance("PKCS12");
                    if (ks.exists()) {
                        if (ks.length() == 0) {
                            keys.load(null);
                        } else {
                            keys.load(new FileInputStream(ks), "pass".toCharArray());
                        }
                    } else {
                        // assuming ocsp is disabled
                        return;
                    }
                } catch (GeneralSecurityException e) {
                    throw new Error(e);
                } catch (IOException e) {
                    throw new Error(e);
                }
                Map<String, OCSPIssuer> toServe = new HashMap<>();

                scanAndUpdateCAs(f, keys, toServe);
                try {
                    keys.store(new FileOutputStream(ks), "pass".toCharArray());
                } catch (GeneralSecurityException e) {
                    throw new Error(e);
                } catch (IOException e) {
                    throw new Error(e);
                }

                try {
                    Map<AlgorithmId, Map<OCSPIssuerId, OCSPIssuer>> map = new HashMap<>();
                    index(AlgorithmId.get("SHA-1"), MessageDigest.getInstance("SHA-1"), toServe, map);
                    index(AlgorithmId.get("SHA-256"), MessageDigest.getInstance("SHA-256"), toServe, map);
                    synchronized (this) {
                        this.map = Collections.unmodifiableMap(map);
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                long dt = Math.max(3000, nextHousekeeping - System.currentTimeMillis());
                Thread.sleep(dt);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized Map<OCSPIssuerId, OCSPIssuer> get(AlgorithmId alg) {
        return map.get(alg);
    }
}
