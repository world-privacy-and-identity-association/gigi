package club.wpia.gigi.dbObjects;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.DBEnum;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.pages.account.certs.CertificateRequest;
import club.wpia.gigi.util.PEM;

public class Certificate implements IdCachable {

    public enum RevocationType implements DBEnum {
        USER("user"), SUPPORT("support"), PING_TIMEOUT("ping_timeout"), KEY_COMPROMISE("key_compromise");

        private final String dbName;

        private RevocationType(String dbName) {
            this.dbName = dbName;
        }

        @Override
        public String getDBName() {
            return dbName;
        }

        public static RevocationType fromString(String s) {
            return valueOf(s.toUpperCase(Locale.ENGLISH));
        }
    }

    public enum AttachmentType implements DBEnum {
        CSR, CRT;

        @Override
        public String getDBName() {
            return toString();
        }
    }

    public enum SANType implements DBEnum {
        EMAIL("email"), DNS("DNS");

        private final String opensslName;

        private SANType(String opensslName) {
            this.opensslName = opensslName;
        }

        public String getOpensslName() {
            return opensslName;
        }

        @Override
        public String getDBName() {
            return opensslName;
        }
    }

    public static class SubjectAlternateName implements Comparable<SubjectAlternateName> {

        private SANType type;

        private String name;

        public SubjectAlternateName(SANType type, String name) {
            this.type = type;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public SANType getType() {
            return type;
        }

        @Override
        public int compareTo(SubjectAlternateName o) {
            int i = type.compareTo(o.type);
            if (i != 0) {
                return i;
            }
            return name.compareTo(o.name);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SubjectAlternateName other = (SubjectAlternateName) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if ( !name.equals(other.name)) {
                return false;
            }
            if (type != other.type) {
                return false;
            }
            return true;
        }

    }

    public enum CSRType {
        CSR, SPKAC;
    }

    private int id;

    private CertificateOwner owner;

    private String serial;

    private Digest md;

    private String csr = null;

    private CSRType csrType;

    private List<SubjectAlternateName> sans;

    private CertificateProfile profile;

    private HashMap<String, String> dn;

    private String dnString;

    private CACertificate ca;

    private String description = "";

    private User actor;

    public static final TranslateCommand NOT_LOADED = new TranslateCommand("Certificate could not be loaded");

    public static final TranslateCommand NOT_PARSED = new TranslateCommand("Certificate could not be parsed");

    /**
     * Creates a new Certificate. WARNING: this is an internal API. Creating
     * certificates for users must be done using the {@link CertificateRequest}
     * -API.
     * 
     * @param owner
     *            the owner for whom the certificate should be created.
     * @param actor
     *            the acting user that creates the certificate
     * @param dn
     *            the distinguished name of the subject of this certificate (as
     *            Map using OpenSSL-Style keys)
     * @param md
     *            the {@link Digest} to sign the certificate with
     * @param csr
     *            the CSR/SPKAC-Request containing the public key in question
     * @param csrType
     *            the type of the csr parameter
     * @param profile
     *            the profile under which this certificate is to be issued
     * @param sans
     *            additional subject alternative names
     * @throws GigiApiException
     *             in case the request is malformed or internal errors occur
     * @throws IOException
     *             when the request cannot be written.
     */
    public Certificate(CertificateOwner owner, User actor, HashMap<String, String> dn, Digest md, String csr, CSRType csrType, CertificateProfile profile, SubjectAlternateName... sans) throws GigiApiException, IOException {
        if ( !profile.canBeIssuedBy(owner, actor)) {
            throw new GigiApiException("You are not allowed to issue these certificates.");
        }
        this.owner = owner;
        this.dn = dn;
        if (dn.size() == 0) {
            throw new GigiApiException("DN must not be empty.");
        }
        dnString = stringifyDN(dn);
        this.md = md;
        this.csr = csr;
        this.csrType = csrType;
        this.profile = profile;
        this.sans = Arrays.asList(sans);
        this.actor = actor;
        synchronized (Certificate.class) {

            try (GigiPreparedStatement inserter = new GigiPreparedStatement("INSERT INTO certs SET md=?::`mdType`, csr_type=?::`csrType`, memid=?, profile=?, actorid=?")) {
                inserter.setString(1, md.toString().toLowerCase());
                inserter.setString(2, this.csrType.toString());
                inserter.setInt(3, owner.getId());
                inserter.setInt(4, profile.getId());
                inserter.setInt(5, this.actor.getId());
                inserter.execute();
                id = inserter.lastInsertId();
            }

            try (GigiPreparedStatement san = new GigiPreparedStatement("INSERT INTO `subjectAlternativeNames` SET `certId`=?, contents=?, type=?::`SANType`")) {
                for (SubjectAlternateName subjectAlternateName : sans) {
                    san.setInt(1, id);
                    san.setString(2, subjectAlternateName.getName());
                    san.setString(3, subjectAlternateName.getType().getOpensslName());
                    san.execute();
                }
            }

            try (GigiPreparedStatement insertAVA = new GigiPreparedStatement("INSERT INTO `certAvas` SET `certId`=?, name=?, value=?")) {
                insertAVA.setInt(1, id);
                for (Entry<String, String> e : this.dn.entrySet()) {
                    insertAVA.setString(2, e.getKey());
                    insertAVA.setString(3, e.getValue());
                    insertAVA.execute();
                }
            }
            addAttachment(AttachmentType.CSR, csr);
            cache.put(this);
        }
    }

    private Certificate(GigiResultSet rs) {
        this.id = rs.getInt("id");
        dnString = rs.getString("subject");
        md = Digest.valueOf(rs.getString("md").toUpperCase());
        owner = CertificateOwner.getById(rs.getInt("memid"));
        profile = CertificateProfile.getById(rs.getInt("profile"));
        this.serial = rs.getString("serial");
        this.description = rs.getString("description");
        this.actor = User.getById(rs.getInt("actorid"));

        try (GigiPreparedStatement ps2 = new GigiPreparedStatement("SELECT `contents`, `type` FROM `subjectAlternativeNames` WHERE `certId`=?")) {
            ps2.setInt(1, id);
            GigiResultSet rs2 = ps2.executeQuery();
            sans = new LinkedList<>();
            while (rs2.next()) {
                sans.add(new SubjectAlternateName(SANType.valueOf(rs2.getString("type").toUpperCase()), rs2.getString("contents")));
            }
        }
    }

    public enum CertificateStatus {
        /**
         * This certificate is not in the database, has no id and only exists as
         * this java object.
         */
        DRAFT("draft"),
        /**
         * The certificate has been signed. It is stored in the database.
         * {@link Certificate#cert()} is valid.
         */
        ISSUED("issued"),

        /**
         * The certificate has been revoked.
         */
        REVOKED("revoked"),

        /**
         * If this certificate cannot be updated because an error happened in
         * the signer.
         */
        ERROR("error");

        private final Outputable name;

        private CertificateStatus(String codename) {
            this.name = new TranslateCommand(codename);

        }

        public Outputable getName() {
            return name;
        }

    }

    public synchronized CertificateStatus getStatus() {
        try (GigiPreparedStatement searcher = new GigiPreparedStatement("SELECT created, revoked, serial, caid FROM certs WHERE id=?")) {
            searcher.setInt(1, id);
            GigiResultSet rs = searcher.executeQuery();
            if ( !rs.next()) {
                throw new IllegalStateException("Certificate not in Database");
            }

            serial = rs.getString(3);
            if (rs.getTimestamp(1) == null) {
                return CertificateStatus.DRAFT;
            }
            ca = CACertificate.getById(rs.getInt("caid"));
            if (rs.getTimestamp(1) != null && rs.getTimestamp(2) == null) {
                return CertificateStatus.ISSUED;
            }
            return CertificateStatus.REVOKED;
        }
    }

    /**
     * @param start
     *            the date from which on the certificate should be valid. (or
     *            null if it should be valid instantly)
     * @param period
     *            the period for which the date should be valid. (a
     *            <code>yyyy-mm-dd</code> or a "2y" (2 calendar years), "6m" (6
     *            months)
     * @return A job which can be used to monitor the progress of this task.
     * @throws IOException
     *             for problems with writing the CSR/SPKAC
     * @throws GigiApiException
     *             if the period is bogus
     */
    public Job issue(Date start, String period, User actor) throws IOException, GigiApiException {
        if (getStatus() != CertificateStatus.DRAFT) {
            throw new IllegalStateException();
        }

        return Job.sign(this, start, period);

    }

    public Job revoke(RevocationType type) {
        if (getStatus() != CertificateStatus.ISSUED) {
            throw new IllegalStateException();
        }
        return Job.revoke(this, type);
    }

    public Job revoke(String challenge, String signature, String message) {
        if (getStatus() != CertificateStatus.ISSUED) {
            throw new IllegalStateException();
        }
        return Job.revoke(this, challenge, signature, message);
    }

    public CACertificate getParent() {
        CertificateStatus status = getStatus();
        if (status != CertificateStatus.REVOKED && status != CertificateStatus.ISSUED) {
            throw new IllegalStateException(status + " is not wanted here.");
        }
        return ca;
    }

    public X509Certificate cert() throws IOException, GeneralSecurityException, GigiApiException {
        CertificateStatus status = getStatus();
        if (status != CertificateStatus.REVOKED && status != CertificateStatus.ISSUED) {
            throw new IllegalStateException(status + " is not wanted here.");
        }
        String crtS = getAttachment(AttachmentType.CRT);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(PEM.decode("CERTIFICATE", crtS))) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(bais);
        }
    }

    public Certificate renew() {
        return null;
    }

    public int getId() {
        return id;
    }

    public String getSerial() {
        getStatus();
        // poll changes
        return serial;
    }

    public String getDistinguishedName() {
        return dnString;
    }

    public Digest getMessageDigest() {
        return md;
    }

    public CertificateOwner getOwner() {
        return owner;
    }

    public List<SubjectAlternateName> getSANs() {
        return Collections.unmodifiableList(sans);
    }

    public CertificateProfile getProfile() {
        return profile;
    }

    private static final String CONCAT = "string_agg(concat('/', `name`, '=', REPLACE(REPLACE(value, '\\\\', '\\\\\\\\'), '/', '\\\\/')), '')";

    public synchronized static Certificate getBySerial(BigInteger serial) {
        if (serial == null) {
            return null;
        }
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT certs.id, " + CONCAT + " as `subject`, `md`,`memid`, `profile`, `certs`.`serial`, `certs`.`description`, `certs`.`actorid` FROM `certs` LEFT JOIN `certAvas` ON `certAvas`.`certId`=`certs`.`id` WHERE `serial`=? GROUP BY `certs`.`id`")) {
            ps.setString(1, serial.toString(16));
            GigiResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                return null;
            }
            int id = rs.getInt(1);
            Certificate c1 = cache.get(id);
            if (c1 != null) {
                return c1;
            }
            Certificate certificate = new Certificate(rs);
            cache.put(certificate);
            return certificate;
        }
    }

    private static ObjectCache<Certificate> cache = new ObjectCache<>();

    public synchronized static Certificate getById(int id) {
        Certificate cacheRes = cache.get(id);
        if (cacheRes != null) {
            return cacheRes;
        }

        try {
            try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT certs.id, " + CONCAT + " as subject, md, memid, profile, certs.serial, description, actorid FROM `certs` LEFT JOIN `certAvas` ON `certAvas`.`certId`=certs.id WHERE certs.id=? GROUP BY certs.id")) {
                ps.setInt(1, id);
                GigiResultSet rs = ps.executeQuery();
                if ( !rs.next()) {
                    return null;
                }

                Certificate c = new Certificate(rs);
                cache.put(c);
                return c;
            }
        } catch (IllegalArgumentException e) {

        }
        return null;
    }

    public static String escapeAVA(String value) {

        return value.replace("\\", "\\\\").replace("/", "\\/");
    }

    public static String stringifyDN(HashMap<String, String> contents) {
        StringBuffer res = new StringBuffer();
        for (Entry<String, String> i : contents.entrySet()) {
            res.append("/" + i.getKey() + "=");
            res.append(escapeAVA(i.getValue()));
        }
        return res.toString();
    }

    public static HashMap<String, String> buildDN(String... contents) {
        HashMap<String, String> res = new HashMap<>();
        for (int i = 0; i + 1 < contents.length; i += 2) {
            res.put(contents[i], contents[i + 1]);
        }
        return res;
    }

    public java.util.Date getRevocationDate() {
        if (getStatus() == CertificateStatus.REVOKED) {
            try (GigiPreparedStatement prep = new GigiPreparedStatement("SELECT revoked FROM certs WHERE id=?")) {
                prep.setInt(1, getId());
                GigiResultSet res = prep.executeQuery();
                if (res.next()) {
                    return new java.util.Date(res.getTimestamp("revoked").getTime());
                }
            }
        }
        return null;
    }

    public java.util.Date getExpiryDate() {
        if (getStatus() == CertificateStatus.ISSUED) {
            try (GigiPreparedStatement prep = new GigiPreparedStatement("SELECT expire FROM certs WHERE id=?")) {
                prep.setInt(1, getId());
                GigiResultSet res = prep.executeQuery();
                if (res.next()) {
                    return res.getTimestamp("expire");
                }
            }
        }
        return null;
    }

    public void setLoginEnabled(boolean activate) {
        if (activate) {
            if ( !isLoginEnabled()) {
                try (GigiPreparedStatement prep = new GigiPreparedStatement("INSERT INTO `logincerts` SET `id`=?")) {
                    prep.setInt(1, id);
                    prep.execute();
                }
            }
        } else {
            try (GigiPreparedStatement prep = new GigiPreparedStatement("DELETE FROM `logincerts` WHERE `id`=?")) {
                prep.setInt(1, id);
                prep.execute();
            }
        }
    }

    public boolean isLoginEnabled() {
        try (GigiPreparedStatement prep = new GigiPreparedStatement("SELECT 1 FROM `logincerts` WHERE `id`=?")) {
            prep.setInt(1, id);
            GigiResultSet res = prep.executeQuery();
            return res.next();
        }
    }

    public static Certificate[] findBySerialPattern(String serial) {
        try (GigiPreparedStatement prep = new GigiPreparedStatement("SELECT `id` FROM `certs` WHERE `serial` LIKE ? GROUP BY `id`  LIMIT 100", true)) {
            prep.setString(1, serial);
            return fetchCertsToArray(prep);
        }
    }

    public static Certificate[] findBySANPattern(String request, SANType type) {
        try (GigiPreparedStatement prep = new GigiPreparedStatement("SELECT `certId` FROM `subjectAlternativeNames` WHERE `contents` LIKE ? and `type`=?::`SANType` GROUP BY `certId` LIMIT 100", true)) {
            prep.setString(1, request);
            prep.setEnum(2, type);
            return fetchCertsToArray(prep);
        }
    }

    private static Certificate[] fetchCertsToArray(GigiPreparedStatement prep) {
        GigiResultSet res = prep.executeQuery();
        res.last();
        Certificate[] certs = new Certificate[res.getRow()];
        res.beforeFirst();
        for (int i = 0; res.next(); i++) {
            certs[i] = Certificate.getById(res.getInt(1));
        }
        return certs;
    }

    public void addAttachment(AttachmentType tp, String data) throws GigiApiException {
        if (getAttachment(tp) != null) {
            throw new GigiApiException("Cannot override attachment");
        }
        if (data == null) {
            throw new GigiApiException("Attachment must not be null");
        }
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `certificateAttachment` SET `certid`=?, `type`=?::`certificateAttachmentType`, `content`=?")) {
            ps.setInt(1, getId());
            ps.setEnum(2, tp);
            ps.setString(3, data);
            ps.execute();
        }
    }

    public String getAttachment(AttachmentType tp) throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `content` FROM `certificateAttachment` WHERE `certid`=? AND `type`=?::`certificateAttachmentType`")) {
            ps.setInt(1, getId());
            ps.setEnum(2, tp);
            GigiResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                return null;
            }
            String s = rs.getString(1);
            if (rs.next()) {
                throw new GigiApiException("Invalid database state");
            }
            return s;
        }
    }

    public void setDescription(String description) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `certs` SET `description`=? WHERE `id`=?")) {
            ps.setString(1, description);
            ps.setInt(2, id);
            ps.execute();
        }
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public User getActor() {
        return actor;
    }

    public static Certificate locateCertificate(String serial, String certData) throws GigiApiException {
        if (serial != null && !serial.isEmpty()) {
            return getBySerial(normalizeSerial(serial));
        }

        if (certData != null && !certData.isEmpty()) {
            final byte[] supplied;
            final X509Certificate c0;
            try {
                supplied = PEM.decode("CERTIFICATE", certData);
                c0 = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(supplied));
            } catch (IllegalArgumentException e1) {
                throw new GigiApiException(NOT_PARSED);
            } catch (CertificateException e1) {
                throw new GigiApiException(NOT_PARSED);
            }
            try {
                Certificate c = getBySerial(c0.getSerialNumber());
                if (c == null) {
                    return null;
                }
                X509Certificate cert = c.cert();
                if ( !Arrays.equals(supplied, cert.getEncoded())) {
                    return null;
                }
                return c;
            } catch (IOException e) {
                throw new GigiApiException(NOT_LOADED);
            } catch (GeneralSecurityException e) {
                throw new GigiApiException(NOT_LOADED);
            }
        }
        throw new GigiApiException("No information to identify the correct certificate was provided.");
    }

    public static BigInteger normalizeSerial(String serial) throws GigiApiException {
        serial = serial.replace(" ", "");
        serial = serial.toLowerCase();
        if (serial.matches("[0-9a-f]{2}(:[0-9a-f]{2})*")) {
            serial = serial.replace(":", "");
        }
        int idx = 0;
        while (idx < serial.length() && serial.charAt(idx) == '0') {
            idx++;
        }
        serial = serial.substring(idx);
        if ( !serial.matches("[0-9a-f]+")) {
            throw new GigiApiException("Malformed serial");
        }
        return new BigInteger(serial, 16);
    }

    public String getFingerprint(String algorithm) throws IOException, GeneralSecurityException, GigiApiException {
        X509Certificate certx = cert();
        return getFingerprint(certx, algorithm);
    }

    protected static String getFingerprint(X509Certificate cert, String algorithm) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        String digestHex = DatatypeConverter.printHexBinary(digest);
        return digestHex.toLowerCase();
    }
}
