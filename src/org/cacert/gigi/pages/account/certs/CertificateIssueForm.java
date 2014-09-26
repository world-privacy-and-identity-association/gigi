package org.cacert.gigi.pages.account.certs;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.CertificateValiditySelector;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.HashAlgorithms;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.PEM;
import org.cacert.gigi.util.RandomToken;

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

/**
 * This class represents a form that is used for issuing certificates. This
 * class uses "sun.security" and therefore needs "-XDignore.symbol.file"
 */
public class CertificateIssueForm extends Form {

    public static final String DEFAULT_CN = "CAcert WoT User";

    private final static Template t = new Template(CertificateIssueForm.class.getResource("CertificateIssueForm.templ"));

    private final static Template tIni = new Template(CertificateAdd.class.getResource("RequestCertificate.templ"));

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

    private User u;

    private CSRType csrType;

    private String csr;

    private String spkacChallenge;

    public String CN = DEFAULT_CN;

    private Set<SubjectAlternateName> SANs = new LinkedHashSet<>();

    private Digest selectedDigest = Digest.getDefault();

    CertificateValiditySelector issueDate = new CertificateValiditySelector();

    private boolean login;

    private CertificateProfile profile = CertificateProfile.getById(1);

    public CertificateIssueForm(HttpServletRequest hsr) {
        super(hsr);
        u = Page.getUser(hsr);
        spkacChallenge = RandomToken.generateToken(16);
    }

    private Certificate result;

    public Certificate getResult() {
        return result;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        String csr = req.getParameter("CSR");
        String spkac = req.getParameter("SPKAC");
        try {
            try {
                if (csr != null) {
                    byte[] data = PEM.decode("(NEW )?CERTIFICATE REQUEST", csr);
                    PKCS10 parsed = new PKCS10(data);
                    PKCS10Attributes atts = parsed.getAttributes();

                    for (PKCS10Attribute b : atts.getAttributes()) {

                        if ( !b.getAttributeId().equals((Object) PKCS9Attribute.EXTENSION_REQUEST_OID)) {
                            // unknown attrib
                            continue;
                        }

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
                                    // parse invalid SANs
                                }
                            }
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
                    out.println(parsed.getSubjectName().getCommonName());
                    out.println(parsed.getSubjectName().getCountry());

                    out.println("CSR DN: " + parsed.getSubjectName() + "<br/>");
                    PublicKey pk = parsed.getSubjectPublicKeyInfo();
                    checkKeyStrength(pk, out);
                    String sign = getSignatureAlgorithm(data);
                    guessDigest(sign);

                    out.println("<br/>digest: " + sign + "<br/>");

                    this.csr = csr;
                    this.csrType = CSRType.CSR;
                } else if (spkac != null) {
                    String cleanedSPKAC = spkac.replaceAll("[\r\n]", "");
                    byte[] data = Base64.getDecoder().decode(cleanedSPKAC);
                    SPKAC parsed = new SPKAC(data);
                    if ( !parsed.getChallenge().equals(spkacChallenge)) {
                        throw new GigiApiException("Challenge mismatch");
                    }
                    checkKeyStrength(parsed.getPubkey(), out);
                    String sign = getSignatureAlgorithm(data);
                    guessDigest(sign);
                    out.println("<br/>digest: " + sign + "<br/>");

                    // spkacChallenge
                    this.csr = "SPKAC=" + cleanedSPKAC;
                    this.csrType = CSRType.SPKAC;

                } else {
                    login = "1".equals(req.getParameter("login"));
                    issueDate.update(req);
                    CN = req.getParameter("CN");
                    String hashAlg = req.getParameter("hash_alg");
                    if (hashAlg != null) {
                        selectedDigest = Digest.valueOf(hashAlg);
                    }
                    profile = CertificateProfile.getByName(req.getParameter("profile"));
                    if ( !u.canIssue(profile)) {
                        profile = CertificateProfile.getById(1);
                        outputError(out, req, "Certificate Profile is invalid.");
                        return false;
                    }

                    String pDNS = null;
                    String pMail = null;
                    Set<SubjectAlternateName> filteredSANs = new LinkedHashSet<>();
                    boolean server = profile.getKeyName().equals("server");
                    for (SubjectAlternateName san : parseSANBox(req.getParameter("SANs"))) {
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
                        outputError(out, req, "The requested Subject alternate name \"%s\" has been removed.",//
                                san.getType().toString().toLowerCase() + ":" + san.getName());
                    }
                    SANs = filteredSANs;
                    if ( !u.isValidName(CN) && !server && !CN.equals(DEFAULT_CN)) {
                        CN = DEFAULT_CN;
                        outputError(out, req, "The real name entered cannot be verified with your account.");
                    }

                    final StringBuffer subject = new StringBuffer();
                    if (server && pDNS != null) {
                        subject.append("/commonName=");
                        subject.append(pDNS);
                        if (pMail != null) {
                            outputError(out, req, "No email is included in this certificate.");
                        }
                        if (CN.equals("")) {
                            CN = "";
                            outputError(out, req, "No real name is included in this certificate.");
                        }
                    } else {
                        subject.append("/commonName=");
                        subject.append(CN);
                        if (pMail != null) {
                            subject.append("/emailAddress=");
                            subject.append(pMail);
                        }
                    }
                    if (req.getParameter("CCA") == null) {
                        outputError(out, req, "You need to accept the CCA.");
                    }
                    if (isFailed(out)) {
                        return false;
                    }

                    result = new Certificate(LoginPage.getUser(req), subject.toString(), selectedDigest.toString(), //
                            this.csr, this.csrType, profile, SANs.toArray(new SubjectAlternateName[SANs.size()]));
                    result.issue(issueDate.getFrom(), issueDate.getTo()).waitFor(60000);
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                throw new GigiApiException("Certificate Request format is invalid.");
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                throw new GigiApiException("Certificate Request format is invalid.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
        }
        return false;
    }

    private void guessDigest(String sign) {
        if (sign.toLowerCase().startsWith("sha512")) {
            selectedDigest = Digest.SHA512;
        } else if (sign.toLowerCase().startsWith("sha384")) {
            selectedDigest = Digest.SHA384;
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

    private void checkKeyStrength(PublicKey pk, PrintWriter out) {
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

    private static String getSignatureAlgorithm(byte[] data) throws IOException {
        DerInputStream in = new DerInputStream(data);
        DerValue[] seq = in.getSequence(3);
        return AlgorithmId.parse(seq[1]).getName();
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        if (csr == null) {
            HashMap<String, Object> vars2 = new HashMap<String, Object>(vars);
            vars2.put("csrf", getCSRFToken());
            vars2.put("csrf_name", getCsrfFieldName());
            vars2.put("spkacChallenge", spkacChallenge);
            tIni.output(out, l, vars2);
            return;
        } else {
            super.output(out, l, vars);
        }
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        HashMap<String, Object> vars2 = new HashMap<String, Object>(vars);
        vars2.put("CCA", "<a href='/policy/CAcertCommunityAgreement.html'>CCA</a>");

        StringBuffer content = new StringBuffer();
        for (SubjectAlternateName SAN : SANs) {
            content.append(SAN.getType().toString().toLowerCase());
            content.append(':');
            content.append(SAN.getName());
            content.append('\n');
        }

        vars2.put("CN", CN);
        vars2.put("validity", issueDate);
        vars2.put("emails", content.toString());
        vars2.put("hashs", new HashAlgorithms(selectedDigest));
        vars2.put("profiles", new IterableDataset() {

            int i = 1;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                CertificateProfile cp;
                do {
                    cp = CertificateProfile.getById(i++);
                    if (cp == null) {
                        return false;
                    }
                } while ( !u.canIssue(cp));

                if (cp.getId() == profile.getId()) {
                    vars.put("selected", " selected");
                } else {
                    vars.put("selected", "");
                }
                vars.put("key", cp.getKeyName());
                vars.put("name", cp.getVisibleName());
                return true;
            }
        });
        t.output(out, l, vars2);
    }
}
