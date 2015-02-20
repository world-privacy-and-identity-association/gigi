package org.cacert.gigi.pages.account.certs;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.crypto.SPKAC;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.Certificate.SANType;
import org.cacert.gigi.dbObjects.Certificate.SubjectAlternateName;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.template.Scope;
import org.cacert.gigi.output.template.SprintfCommand;
import org.cacert.gigi.util.PEM;

import sun.security.pkcs.PKCS9Attribute;
import sun.security.pkcs10.PKCS10;
import sun.security.pkcs10.PKCS10Attribute;
import sun.security.pkcs10.PKCS10Attributes;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AVA;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.DNSName;
import sun.security.x509.ExtendedKeyUsageExtension;
import sun.security.x509.Extension;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.RDN;
import sun.security.x509.RFC822Name;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X500Name;

public class CertificateRequest {

    public static final String DEFAULT_CN = "CAcert WoT User";

    public static final ObjectIdentifier OID_KEY_USAGE_SSL_SERVER = ObjectIdentifier.newInternal(new int[] {
            1, 3, 6, 1, 5, 5, 7, 3, 1
    });

    public static final ObjectIdentifier OID_KEY_USAGE_SSL_CLIENT = ObjectIdentifier.newInternal(new int[] {
            1, 3, 6, 1, 5, 5, 7, 3, 2
    });

    public static final ObjectIdentifier OID_KEY_USAGE_CODESIGN = ObjectIdentifier.newInternal(new int[] {
            1, 3, 6, 1, 5, 5, 7, 3, 3
    });

    public static final ObjectIdentifier OID_KEY_USAGE_EMAIL_PROTECTION = ObjectIdentifier.newInternal(new int[] {
            1, 3, 6, 1, 5, 5, 7, 3, 4
    });

    public static final ObjectIdentifier OID_KEY_USAGE_TIMESTAMP = ObjectIdentifier.newInternal(new int[] {
            1, 3, 6, 1, 5, 5, 7, 3, 8
    });

    public static final ObjectIdentifier OID_KEY_USAGE_OCSP = ObjectIdentifier.newInternal(new int[] {
            1, 3, 6, 1, 5, 5, 7, 3, 9
    });

    private CSRType csrType;

    private final PublicKey pk;

    private String csr;

    public String CN = DEFAULT_CN;

    private Set<SubjectAlternateName> SANs;

    private Digest selectedDigest = Digest.getDefault();

    private CertificateProfile profile = CertificateProfile.getById(1);

    private String ou = "";

    private Organisation org = null;

    private User u;

    private String pDNS, pMail;

    public CertificateRequest(User issuer, String csr) throws IOException, GeneralSecurityException, GigiApiException {
        u = issuer;
        byte[] data = PEM.decode("(NEW )?CERTIFICATE REQUEST", csr);
        PKCS10 parsed = new PKCS10(data);
        PKCS10Attributes atts = parsed.getAttributes();

        TreeSet<SubjectAlternateName> SANs = new TreeSet<>();
        for (RDN r : parsed.getSubjectName().rdns()) {
            for (AVA a : r.avas()) {
                if (a.getObjectIdentifier().equals((Object) PKCS9Attribute.EMAIL_ADDRESS_OID)) {
                    SANs.add(new SubjectAlternateName(SANType.EMAIL, a.getValueString()));
                } else if (a.getObjectIdentifier().equals((Object) X500Name.commonName_oid)) {
                    String value = a.getValueString();
                    if (value.contains(".") && !value.contains(" ")) {
                        SANs.add(new SubjectAlternateName(SANType.DNS, value));
                    } else {
                        CN = value;
                    }
                } else if (a.getObjectIdentifier().equals((Object) PKIXExtensions.SubjectAlternativeName_Id)) {
                    // TODO? parse invalid SANs
                }
            }
        }

        for (PKCS10Attribute b : atts.getAttributes()) {

            if ( !b.getAttributeId().equals((Object) PKCS9Attribute.EXTENSION_REQUEST_OID)) {
                // unknown attrib
                continue;
            }

            for (Extension c : ((CertificateExtensions) b.getAttributeValue()).getAllExtensions()) {
                if (c instanceof SubjectAlternativeNameExtension) {

                    SubjectAlternativeNameExtension san = (SubjectAlternativeNameExtension) c;
                    GeneralNames obj = san.get(SubjectAlternativeNameExtension.SUBJECT_NAME);
                    for (int i = 0; i < obj.size(); i++) {
                        GeneralName generalName = obj.get(i);
                        GeneralNameInterface peeled = generalName.getName();
                        if (peeled instanceof DNSName) {
                            SANs.add(new SubjectAlternateName(SANType.DNS, ((DNSName) peeled).getName()));
                        } else if (peeled instanceof RFC822Name) {
                            SANs.add(new SubjectAlternateName(SANType.EMAIL, ((RFC822Name) peeled).getName()));
                        }
                    }
                } else if (c instanceof ExtendedKeyUsageExtension) {
                    ExtendedKeyUsageExtension ekue = (ExtendedKeyUsageExtension) c;
                    for (String s : ekue.getExtendedKeyUsage()) {
                        if (s.equals(OID_KEY_USAGE_SSL_SERVER.toString())) {
                            // server
                            profile = CertificateProfile.getByName("server");
                        } else if (s.equals(OID_KEY_USAGE_SSL_CLIENT.toString())) {
                            // client
                            profile = CertificateProfile.getByName("client");
                        } else if (s.equals(OID_KEY_USAGE_CODESIGN.toString())) {
                            // code sign
                        } else if (s.equals(OID_KEY_USAGE_EMAIL_PROTECTION.toString())) {
                            // emailProtection
                            profile = CertificateProfile.getByName("mail");
                        } else if (s.equals(OID_KEY_USAGE_TIMESTAMP.toString())) {
                            // timestamp
                        } else if (s.equals(OID_KEY_USAGE_OCSP.toString())) {
                            // OCSP
                        }
                    }
                } else {
                    // Unknown requested extension
                }
            }

        }
        this.SANs = SANs;
        pk = parsed.getSubjectPublicKeyInfo();
        String sign = getSignatureAlgorithm(data);
        guessDigest(sign);

        this.csr = csr;
        this.csrType = CSRType.CSR;
    }

    public CertificateRequest(User issuer, String spkac, String spkacChallenge) throws IOException, GigiApiException, GeneralSecurityException {
        u = issuer;
        String cleanedSPKAC = spkac.replaceAll("[\r\n]", "");
        byte[] data = Base64.getDecoder().decode(cleanedSPKAC);
        SPKAC parsed = new SPKAC(data);
        if ( !parsed.getChallenge().equals(spkacChallenge)) {
            throw new GigiApiException("Challenge mismatch");
        }
        pk = parsed.getPubkey();
        String sign = getSignatureAlgorithm(data);
        guessDigest(sign);
        this.SANs = new HashSet<>();
        this.csr = "SPKAC=" + cleanedSPKAC;
        this.csrType = CSRType.SPKAC;

    }

    private static String getSignatureAlgorithm(byte[] data) throws IOException {
        DerInputStream in = new DerInputStream(data);
        DerValue[] seq = in.getSequence(3);
        return AlgorithmId.parse(seq[1]).getName();
    }

    private void guessDigest(String sign) {
        if (sign.toLowerCase().startsWith("sha512")) {
            selectedDigest = Digest.SHA512;
        } else if (sign.toLowerCase().startsWith("sha384")) {
            selectedDigest = Digest.SHA384;
        }
    }

    public void checkKeyStrength(PrintWriter out) {
        out.println("Type: " + pk.getAlgorithm() + "<br/>");
        if (pk instanceof RSAPublicKey) {
            out.println("Exponent: " + ((RSAPublicKey) pk).getPublicExponent() + "<br/>");
            out.println("Length: " + ((RSAPublicKey) pk).getModulus().bitLength());
        } else if (pk instanceof DSAPublicKey) {
            DSAPublicKey dpk = (DSAPublicKey) pk;
            out.println("Length: " + dpk.getY().bitLength() + "<br/>");
            out.println(dpk.getParams());
        } else if (pk instanceof ECPublicKey) {
            ECPublicKey epk = (ECPublicKey) pk;
            out.println("Length-x: " + epk.getW().getAffineX().bitLength() + "<br/>");
            out.println("Length-y: " + epk.getW().getAffineY().bitLength() + "<br/>");
            out.println(epk.getParams().getCurve());
        }
    }

    private TreeSet<SubjectAlternateName> parseSANBox(String SANs) {
        String[] SANparts = SANs.split("[\r\n]+|, *");
        TreeSet<SubjectAlternateName> parsedNames = new TreeSet<>();
        for (String SANline : SANparts) {
            String[] parts = SANline.split(":", 2);
            if (parts.length == 1) {
                if (parts[0].trim().equals("")) {
                    continue;
                }
                if (parts[0].contains("@")) {
                    parsedNames.add(new SubjectAlternateName(SANType.EMAIL, parts[0]));
                } else {
                    parsedNames.add(new SubjectAlternateName(SANType.DNS, parts[0]));
                }
                continue;
            }
            try {
                SANType t = Certificate.SANType.valueOf(parts[0].toUpperCase());
                if (t == null) {
                    continue;
                }
                parsedNames.add(new SubjectAlternateName(t, parts[1]));
            } catch (IllegalArgumentException e) {
                // invalid enum type
                continue;
            }
        }
        return parsedNames;
    }

    public Set<SubjectAlternateName> getSANs() {
        return SANs;
    }

    public String getCN() {
        return CN;
    }

    public Organisation getOrg() {
        return org;
    }

    public String getOu() {
        return ou;
    }

    public Digest getSelectedDigest() {
        return selectedDigest;
    }

    public CertificateProfile getProfile() {
        return profile;
    }

    public boolean update(String CNin, String hashAlg, String profileStr, String newOrgStr, String ou, String SANsStr, PrintWriter out, HttpServletRequest req) throws GigiApiException {
        GigiApiException error = new GigiApiException();
        this.CN = CNin;
        if (hashAlg != null) {
            selectedDigest = Digest.valueOf(hashAlg);
        }
        this.profile = CertificateProfile.getByName(profileStr);
        if (newOrgStr != null) {
            Organisation neworg = Organisation.getById(Integer.parseInt(newOrgStr));
            if (neworg == null || u.getOrganisations().contains(neworg)) {
                org = neworg;
            } else {
                error.mergeInto(new GigiApiException("Selected Organisation is not part of your account."));
            }
        }
        this.ou = ou;

        boolean server = profile.getKeyName().equals("server");
        SANs = verifySANs(error, server, parseSANBox(SANsStr));

        if ( !error.isEmpty()) {
            throw error;
        }
        return true;
    }

    private Set<SubjectAlternateName> verifySANs(GigiApiException error, boolean server, Set<SubjectAlternateName> sANs2) {
        Set<SubjectAlternateName> filteredSANs = new LinkedHashSet<>();
        for (SubjectAlternateName san : sANs2) {
            if (san.getType() == SANType.DNS) {
                if (u.isValidDomain(san.getName()) && server) {
                    if (pDNS == null) {
                        pDNS = san.getName();
                    }
                    filteredSANs.add(san);
                    continue;
                }
            } else if (san.getType() == SANType.EMAIL) {
                if (u.isValidEmail(san.getName()) && !server) {
                    if (pMail == null) {
                        pMail = san.getName();
                    }
                    filteredSANs.add(san);
                    continue;
                }
            }
            HashMap<String, Object> vars = new HashMap<>();
            vars.put("SAN", san.getType().toString().toLowerCase() + ":" + san.getName());
            error.mergeInto(new GigiApiException(new Scope(new SprintfCommand(//
                    "The requested Subject alternate name \"%s\" has been removed.", Arrays.asList("$SAN")), vars)));
        }
        return filteredSANs;
    }

    public Certificate draft() throws GigiApiException {

        GigiApiException error = new GigiApiException();
        if ( !u.canIssue(this.profile)) {
            this.profile = CertificateProfile.getById(1);
            error.mergeInto(new GigiApiException("Certificate Profile is invalid."));
            throw error;
        }

        boolean server = profile.getKeyName().equals("server");

        HashMap<String, String> subject = new HashMap<>();
        if (server) {
            if (pDNS != null) {
                subject.put("CN", pDNS);
            } else {
                error.mergeInto(new GigiApiException("Server Certificates require a DNS name."));
            }
            if (pMail != null) {
                error.mergeInto(new GigiApiException("No email is included in this certificate."));
            }
            if ( !CN.equals("")) {
                CN = "";
                this.CN = "";
                error.mergeInto(new GigiApiException("No real name is included in this certificate. The real name, you entered will be ignored."));
            }
        } else {
            if ( !u.isValidName(CN) && !CN.equals(DEFAULT_CN)) {
                this.CN = DEFAULT_CN;
                error.mergeInto(new GigiApiException("The name entered, does not match the details in your account. You cannot issue certificates with this name. Enter a name that matches the one that has been assured in your account."));
            }

            subject.put("CN", this.CN);
            if (pMail != null) {
                subject.put("EMAIL", pMail);
            }
        }
        this.SANs = verifySANs(error, server, SANs);
        if (org != null) {
            subject.put("O", org.getName());
            subject.put("C", org.getState());
            subject.put("ST", org.getProvince());
            subject.put("L", org.getCity());
            subject.put("OU", ou);
        }

        if ( !error.isEmpty()) {
            throw error;
        }
        return new Certificate(u, subject, selectedDigest.toString(), //
                this.csr, this.csrType, profile, SANs.toArray(new SubjectAlternateName[SANs.size()]));
    }
}
