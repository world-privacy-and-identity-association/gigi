package club.wpia.gigi.pages.account.certs;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.crypto.SPKAC;
import club.wpia.gigi.crypto.key.KeyCheck;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Certificate.SANType;
import club.wpia.gigi.dbObjects.Certificate.SubjectAlternateName;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.CertificateProfile;
import club.wpia.gigi.dbObjects.CertificateProfile.PropertyTemplate;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.output.template.SprintfCommand;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.CAA;
import club.wpia.gigi.util.DomainAssessment;
import club.wpia.gigi.util.PEM;
import club.wpia.gigi.util.RateLimit;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.TimeConditions;
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

    public static final String DEFAULT_CN = ServerConstants.getAppName() + " User";

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

    public String name = DEFAULT_CN;

    private Set<SubjectAlternateName> SANs;

    private Digest selectedDigest = Digest.getDefault();

    private CertificateProfile profile = CertificateProfile.getById(1);

    private String ou = "";

    private AuthorizationContext ctx;

    private String pDNS, pMail;

    public CertificateRequest(AuthorizationContext c, String csr) throws IOException, GeneralSecurityException, GigiApiException {
        this(c, csr, (CertificateProfile) null);
    }

    public CertificateRequest(AuthorizationContext ctx, String csr, CertificateProfile cp) throws GeneralSecurityException, IOException, IOException, GigiApiException {
        this.ctx = ctx;
        if (cp != null) {
            profile = cp;
        } else if (ctx.getActor().getVerificationPoints() > 50) {
            profile = CertificateProfile.getByName("client-a");
        }
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
                        name = value;
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
                    String appendix = "";
                    if (ctx.getActor().getVerificationPoints() >= 50) {
                        appendix = "-a";
                    }
                    for (String s : ekue.getExtendedKeyUsage()) {
                        if (s.equals(OID_KEY_USAGE_SSL_SERVER.toString())) {
                            // server
                            profile = CertificateProfile.getByName("server" + appendix);
                        } else if (s.equals(OID_KEY_USAGE_SSL_CLIENT.toString())) {
                            // client
                            profile = CertificateProfile.getByName("client" + appendix);
                        } else if (s.equals(OID_KEY_USAGE_CODESIGN.toString())) {
                            // code sign
                        } else if (s.equals(OID_KEY_USAGE_EMAIL_PROTECTION.toString())) {
                            // emailProtection
                            profile = CertificateProfile.getByName("mail" + appendix);
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
        KeyCheck.checkKey(pk);

        String sign = getSignatureAlgorithm(data);
        guessDigest(sign);

        this.csr = csr;
        this.csrType = CSRType.CSR;
    }

    public CertificateRequest(AuthorizationContext ctx, String spkac, String spkacChallenge) throws IOException, GigiApiException, GeneralSecurityException {
        this.ctx = ctx;
        String cleanedSPKAC = spkac.replaceAll("[\r\n]", "");
        byte[] data = Base64.getDecoder().decode(cleanedSPKAC);
        SPKAC parsed = new SPKAC(data);
        if ( !parsed.getChallenge().equals(spkacChallenge)) {
            throw new GigiApiException("Challenge mismatch");
        }
        pk = parsed.getPubkey();
        KeyCheck.checkKey(pk);

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
        } else if (sign.toLowerCase().startsWith("sha256")) {
            selectedDigest = Digest.SHA256;
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

    private Set<SubjectAlternateName> parseSANBox(String SANs) {
        String[] SANparts = SANs.split("[\r\n]+|, *");
        Set<SubjectAlternateName> parsedNames = new LinkedHashSet<>();
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
                SANType t = Certificate.SANType.valueOf(parts[0].toUpperCase().trim());
                if (t == null) {
                    continue;
                }
                parsedNames.add(new SubjectAlternateName(t, parts[1].trim()));
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

    public String getName() {
        return name;
    }

    public synchronized String getOu() {
        if (ctx.getTarget() instanceof Organisation) {
            return ou;
        }
        throw new IllegalStateException();
    }

    public Digest getSelectedDigest() {
        return selectedDigest;
    }

    public CertificateProfile getProfile() {
        return profile;
    }

    public synchronized boolean update(String nameIn, String hashAlg, String profileStr, String newOrgStr, String ou, String SANsStr) throws GigiApiException {
        GigiApiException error = new GigiApiException();
        this.name = nameIn;
        if (hashAlg != null) {
            selectedDigest = Digest.valueOf(hashAlg);
        }
        this.profile = CertificateProfile.getByName(profileStr);
        if (ctx.getTarget() instanceof Organisation) {
            this.ou = ou;
        }

        if ( !this.profile.canBeIssuedBy(ctx.getTarget(), ctx.getActor())) {
            this.profile = CertificateProfile.getById(1);
            error.mergeInto(new GigiApiException("Certificate Profile is invalid."));
            throw error;
        }

        verifySANs(error, profile, parseSANBox(SANsStr), ctx.getTarget(), ctx.getActor());

        if ( !error.isEmpty()) {
            throw error;
        }
        return true;
    }

    private void verifySANs(GigiApiException error, CertificateProfile p, Set<SubjectAlternateName> sANs2, CertificateOwner owner, User user) {
        Set<SubjectAlternateName> filteredSANs = new LinkedHashSet<>();
        PropertyTemplate domainTemp = p.getTemplates().get("domain");
        PropertyTemplate emailTemp = p.getTemplates().get("email");
        pDNS = null;
        pMail = null;
        for (SubjectAlternateName san : sANs2) {
            if (san.getType() == SANType.DNS) {
                if (domainTemp != null && owner.isValidDomain(san.getName())) {
                    boolean valid;
                    try {
                        DomainAssessment.checkCertifiableDomain(san.getName(), user.isInGroup(Group.CODESIGNING), false);
                        valid = true;
                        if ( !valid || !CAA.verifyDomainAccess(owner, p, san.getName()) || (pDNS != null && !domainTemp.isMultiple())) {
                            // remove
                        } else {
                            if (pDNS == null) {
                                pDNS = san.getName();
                            }
                            filteredSANs.add(san);
                            continue;
                        }
                    } catch (GigiApiException e) {
                        error.mergeInto(e);
                        valid = false;
                    }
                }
            } else if (san.getType() == SANType.EMAIL && emailTemp != null) {
                if (owner.isValidEmail(san.getName())) {
                    if (pMail != null && !emailTemp.isMultiple()) {
                        // remove
                    } else {
                        if (pMail == null) {
                            pMail = san.getName();
                        }
                        filteredSANs.add(san);
                        continue;
                    }
                } else {
                    // remove
                    error.mergeInto(new GigiApiException(SprintfCommand.createSimple(//
                            "The requested subject alternate name email address \"{0}\" needs a verification via email ping within the past {1} months.", san.getType().toString().toLowerCase() + ":" + san.getName(), TimeConditions.getInstance().getEmailPingMonths())));
                    break;
                }
            }
            error.mergeInto(new GigiApiException(SprintfCommand.createSimple(//
                    "The requested subject alternate name (SAN) \"{0}\" has been removed.", san.getType().toString().toLowerCase() + ":" + san.getName())));
        }
        SANs = filteredSANs;
    }

    // domain email name name=WoTUser orga
    public synchronized Certificate draft() throws GigiApiException {

        GigiApiException error = new GigiApiException();

        HashMap<String, String> subject = new HashMap<>();
        PropertyTemplate domainTemp = profile.getTemplates().get("domain");
        PropertyTemplate emailTemp = profile.getTemplates().get("email");
        PropertyTemplate nameTemp = profile.getTemplates().get("name");
        PropertyTemplate wotUserTemp = profile.getTemplates().get("name=WoTUser");
        verifySANs(error, profile, SANs, ctx.getTarget(), ctx.getActor());

        // Ok, let's determine the CN
        // the CN is
        // 1. the user's "real name", iff the real name is to be included i.e.
        // not empty (name), or to be forced to WOTUser

        // 2. the user's "primary domain", iff "1." doesn't match and there is a
        // primary domain. (domainTemp != null)

        String verifiedCN = null;
        if (ctx.getTarget() instanceof Organisation) {
            if ( !name.equals("")) {
                verifiedCN = name;
            }
        } else {
            verifiedCN = verifyName(error, nameTemp, wotUserTemp, verifiedCN);
        }
        if (pDNS == null && domainTemp != null && domainTemp.isRequired()) {
            error.mergeInto(new GigiApiException("Server Certificates require a DNS name."));
        } else if (domainTemp != null && verifiedCN == null) {
            // user may add domains
            verifiedCN = pDNS;
        }
        if (verifiedCN != null) {
            subject.put("CN", verifiedCN);
        }

        if (pMail != null) {
            if (emailTemp != null) {
                subject.put("EMAIL", pMail);
            } else {
                // verify SANs should prevent this
                pMail = null;
                error.mergeInto(new GigiApiException("You may not include an email in this certificate."));
            }
        } else {
            if (emailTemp != null && emailTemp.isRequired()) {
                error.mergeInto(new GigiApiException("You need to include an email in this certificate."));
            }
        }

        if (ctx.getTarget() instanceof Organisation) {
            Organisation org = (Organisation) ctx.getTarget();
            subject.put("O", org.getName());
            subject.put("C", org.getCountry().getCode());
            subject.put("ST", org.getProvince());
            subject.put("L", org.getCity());
            if (ou != null) {
                subject.put("OU", ou);
            }
        }

        if ( !error.isEmpty()) {
            throw error;
        }
        try {
            if (RATE_LIMIT.isLimitExceeded(Integer.toString(ctx.getActor().getId()))) {
                throw new GigiApiException("Rate Limit Exceeded");
            }
            return new Certificate(ctx.getTarget(), ctx.getActor(), subject, selectedDigest, //
                    this.csr, this.csrType, profile, SANs.toArray(new SubjectAlternateName[SANs.size()]));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 100 per 10 minutes
    public static final RateLimit RATE_LIMIT = new RateLimit(100, 10 * 60 * 1000);

    private String verifyName(GigiApiException error, PropertyTemplate nameTemp, PropertyTemplate wotUserTemp, String verifiedCN) {
        // real names,
        // possible configurations: name {y,null,?}, name=WoTUser {y,null}
        // semantics:
        // y * -> real
        // null y -> default
        // null null -> null
        // ? y -> real, default
        // ? null -> real, default, null
        boolean realIsOK = false;
        boolean nullIsOK = false;
        boolean defaultIsOK = false;
        if (wotUserTemp != null && ( !wotUserTemp.isRequired() || wotUserTemp.isMultiple())) {
            error.mergeInto(new GigiApiException("Internal configuration error detected."));
        }
        if (nameTemp != null && nameTemp.isRequired() && !nameTemp.isMultiple()) {
            realIsOK = true;
        } else if (nameTemp == null) {
            defaultIsOK = wotUserTemp != null;
            nullIsOK = !defaultIsOK;
        } else if (nameTemp != null && !nameTemp.isRequired() && !nameTemp.isMultiple()) {
            realIsOK = true;
            defaultIsOK = true;
            nullIsOK = wotUserTemp == null;
        } else {
            error.mergeInto(new GigiApiException("Internal configuration error detected."));
        }
        if (ctx.getTarget() instanceof User) {
            User u = (User) ctx.getTarget();
            if (name != null && u.isValidName(name)) {
                if (realIsOK) {
                    if (u.isValidNameVerification(name)) {
                        verifiedCN = name;
                    } else {
                        error.mergeInto(new GigiApiException(SprintfCommand.createSimple("The entered name needs a valid verification within the last {0} months.", TimeConditions.getInstance().getVerificationMonths())));
                    }
                } else {
                    error.mergeInto(new GigiApiException("Your real name is not allowed in this certificate."));
                    if (defaultIsOK) {
                        name = DEFAULT_CN;
                    } else if (nullIsOK) {
                        name = "";
                    }
                }
            } else if (name != null && name.equals(DEFAULT_CN)) {
                if (defaultIsOK) {
                    verifiedCN = name;
                } else {
                    error.mergeInto(new GigiApiException("The default name is not allowed in this certificate."));
                    if (nullIsOK) {
                        name = "";
                    } else if (realIsOK) {
                        name = u.getPreferredName().toString();
                    }
                }
            } else if (name == null || name.equals("")) {
                if (nullIsOK) {
                    verifiedCN = "";
                } else {
                    error.mergeInto(new GigiApiException("A name is required in this certificate."));
                    if (defaultIsOK) {
                        name = DEFAULT_CN;
                    } else if (realIsOK) {
                        name = u.getPreferredName().toString();
                    }
                }
            } else {
                error.mergeInto(new GigiApiException("The name you entered was invalid."));

            }
            if (wotUserTemp != null) {
                if ( !wotUserTemp.isRequired() || wotUserTemp.isMultiple()) {
                    error.mergeInto(new GigiApiException("Internal configuration error detected."));
                }
                if ( !name.equals(DEFAULT_CN)) {
                    name = DEFAULT_CN;
                    error.mergeInto(new GigiApiException("You may not change the name for this certificate type."));
                } else {
                    verifiedCN = DEFAULT_CN;
                }

            } else {
                if (nameTemp != null) {
                    if (name.equals("")) {
                        if (nameTemp.isRequired()) {
                            // nothing, but required
                            name = DEFAULT_CN;
                            error.mergeInto(new GigiApiException("No name entered, but one was required."));
                        } else {
                            // nothing and not required

                        }
                    } else if (u.isValidName(name)) {
                        verifiedCN = name;
                    } else {
                        if (nameTemp.isRequired()) {
                            error.mergeInto(new GigiApiException("The name entered, does not match the details in your account. You cannot issue certificates with this name. Enter a name that matches the one that has been verified in your account, because a name is required for this certificate type."));
                        } else if (name.equals(DEFAULT_CN)) {
                            verifiedCN = DEFAULT_CN;
                        } else {
                            name = DEFAULT_CN;
                            error.mergeInto(new GigiApiException("The name entered, does not match the details in your account. You cannot issue certificates with this name. Enter a name that matches the one that has been verified in your account or keep the default name."));
                        }
                    }
                } else {
                    if ( !name.equals("")) {
                        name = "";
                        error.mergeInto(new GigiApiException("No real name is included in this certificate. The real name, you entered will be ignored."));
                    }
                }
            }
        } else {
            if (realIsOK) {
                verifiedCN = name;
            } else {
                verifiedCN = "";
                name = "";
                error.mergeInto(new GigiApiException("No real name is included in this certificate. The real name, you entered will be ignored."));
            }
        }

        return verifiedCN;
    }
}
