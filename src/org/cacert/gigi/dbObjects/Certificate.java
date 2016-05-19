package org.cacert.gigi.dbObjects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.TranslateCommand;
import org.cacert.gigi.util.KeyStorage;
import org.cacert.gigi.util.Notary;

public class Certificate implements IdCachable {

    public enum SANType {
        EMAIL("email"), DNS("DNS");

        private final String opensslName;

        private SANType(String opensslName) {
            this.opensslName = opensslName;
        }

        public String getOpensslName() {
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

    private String csrName;

    private String crtName;

    private String csr = null;

    private CSRType csrType;

    private List<SubjectAlternateName> sans;

    private CertificateProfile profile;

    private HashMap<String, String> dn;

    private String dnString;

    private CACertificate ca;

    public Certificate(CertificateOwner owner, User actor, HashMap<String, String> dn, Digest md, String csr, CSRType csrType, CertificateProfile profile, SubjectAlternateName... sans) throws GigiApiException, IOException {
        if ( !profile.canBeIssuedBy(owner, actor)) {
            throw new GigiApiException("You are not allowed to issue these certificates.");
        }
        this.owner = owner;
        this.dn = dn;
        if (dn.size() == 0) {
            throw new GigiApiException("DN must not be empty");
        }
        dnString = stringifyDN(dn);
        this.md = md;
        this.csr = csr;
        this.csrType = csrType;
        this.profile = profile;
        this.sans = Arrays.asList(sans);
        synchronized (Certificate.class) {

            try (GigiPreparedStatement inserter = new GigiPreparedStatement("INSERT INTO certs SET md=?::`mdType`, csr_type=?::`csrType`, crt_name='', memid=?, profile=?")) {
                inserter.setString(1, md.toString().toLowerCase());
                inserter.setString(2, csrType.toString());
                inserter.setInt(3, owner.getId());
                inserter.setInt(4, profile.getId());
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
                for (Entry<String, String> e : dn.entrySet()) {
                    insertAVA.setString(2, e.getKey());
                    insertAVA.setString(3, e.getValue());
                    insertAVA.execute();
                }
            }
            File csrFile = KeyStorage.locateCsr(id);
            csrName = csrFile.getPath();
            try (FileOutputStream fos = new FileOutputStream(csrFile)) {
                fos.write(csr.getBytes("UTF-8"));
            }
            try (GigiPreparedStatement updater = new GigiPreparedStatement("UPDATE `certs` SET `csr_name`=? WHERE id=?")) {
                updater.setString(1, csrName);
                updater.setInt(2, id);
                updater.execute();
            }

            cache.put(this);
        }
    }

    private Certificate(GigiResultSet rs) {
        this.id = rs.getInt("id");
        dnString = rs.getString("subject");
        md = Digest.valueOf(rs.getString("md").toUpperCase());
        csrName = rs.getString("csr_name");
        crtName = rs.getString("crt_name");
        owner = CertificateOwner.getById(rs.getInt("memid"));
        profile = CertificateProfile.getById(rs.getInt("profile"));
        this.serial = rs.getString("serial");

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
        try (GigiPreparedStatement searcher = new GigiPreparedStatement("SELECT crt_name, created, revoked, serial, caid FROM certs WHERE id=?")) {
            searcher.setInt(1, id);
            GigiResultSet rs = searcher.executeQuery();
            if ( !rs.next()) {
                throw new IllegalStateException("Certificate not in Database");
            }

            crtName = rs.getString(1);
            serial = rs.getString(4);
            if (rs.getTimestamp(2) == null) {
                return CertificateStatus.DRAFT;
            }
            ca = CACertificate.getById(rs.getInt("caid"));
            if (rs.getTimestamp(2) != null && rs.getTimestamp(3) == null) {
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
        Notary.writeUserAgreement(actor, "CCA", "issue certificate", "", true, 0);

        return Job.sign(this, start, period);

    }

    public Job revoke() {
        if (getStatus() != CertificateStatus.ISSUED) {
            throw new IllegalStateException();
        }
        return Job.revoke(this);

    }

    public CACertificate getParent() {
        CertificateStatus status = getStatus();
        if (status != CertificateStatus.REVOKED && status != CertificateStatus.ISSUED) {
            throw new IllegalStateException(status + " is not wanted here.");
        }
        return ca;
    }

    public X509Certificate cert() throws IOException, GeneralSecurityException {
        CertificateStatus status = getStatus();
        if (status != CertificateStatus.REVOKED && status != CertificateStatus.ISSUED) {
            throw new IllegalStateException(status + " is not wanted here.");
        }
        InputStream is = null;
        X509Certificate crt = null;
        try {
            is = new FileInputStream(crtName);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            crt = (X509Certificate) cf.generateCertificate(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return crt;
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

    public synchronized static Certificate getBySerial(String serial) {
        if (serial == null || "".equals(serial)) {
            return null;
        }
        String concat = "string_agg(concat('/', `name`, '=', REPLACE(REPLACE(value, '\\\\', '\\\\\\\\'), '/', '\\\\/')), '')";
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT certs.id, " + concat + " as `subject`, `md`, `csr_name`, `crt_name`,`memid`, `profile`, `certs`.`serial` FROM `certs` LEFT JOIN `certAvas` ON `certAvas`.`certId`=`certs`.`id` WHERE `serial`=? GROUP BY `certs`.`id`")) {
            ps.setString(1, serial);
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
            String concat = "string_agg(concat('/', `name`, '=', REPLACE(REPLACE(value, '\\\\', '\\\\\\\\'), '/', '\\\\/')), '')";
            try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT certs.id, " + concat + " as subject, md, csr_name, crt_name,memid, profile, certs.serial FROM `certs` LEFT JOIN `certAvas` ON `certAvas`.`certId`=certs.id WHERE certs.id=? GROUP BY certs.id")) {
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
                    return new java.util.Date(res.getDate("revoked").getTime());
                }
            }
        }
        return null;
    }
}
