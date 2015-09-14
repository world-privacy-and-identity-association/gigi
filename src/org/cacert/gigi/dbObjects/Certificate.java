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
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.util.Job;
import org.cacert.gigi.util.KeyStorage;
import org.cacert.gigi.util.Notary;

public class Certificate {

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

    private User owner;

    private String serial;

    private String md;

    private String csrName;

    private String crtName;

    private String csr = null;

    private CSRType csrType;

    private List<SubjectAlternateName> sans;

    private CertificateProfile profile;

    private HashMap<String, String> dn;

    private String dnString;

    private CACertificate ca;

    public Certificate(User owner, HashMap<String, String> dn, String md, String csr, CSRType csrType, CertificateProfile profile, SubjectAlternateName... sans) throws GigiApiException {
        if ( !profile.canBeIssuedBy(owner)) {
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
    }

    private Certificate(GigiResultSet rs) {
        //
        if ( !rs.next()) {
            throw new IllegalArgumentException("Invalid mid " + serial);
        }
        this.id = rs.getInt("id");
        dnString = rs.getString("subject");
        md = rs.getString("md");
        csrName = rs.getString("csr_name");
        crtName = rs.getString("crt_name");
        owner = User.getById(rs.getInt("memid"));
        profile = CertificateProfile.getById(rs.getInt("profile"));
        this.serial = rs.getString("serial");

        GigiPreparedStatement ps2 = DatabaseConnection.getInstance().prepare("SELECT `contents`, `type` FROM `subjectAlternativeNames` WHERE `certId`=?");
        ps2.setInt(1, id);
        GigiResultSet rs2 = ps2.executeQuery();
        sans = new LinkedList<>();
        while (rs2.next()) {
            sans.add(new SubjectAlternateName(SANType.valueOf(rs2.getString("type").toUpperCase()), rs2.getString("contents")));
        }
        rs2.close();

        rs.close();
    }

    public enum CertificateStatus {
        /**
         * This certificate is not in the database, has no id and only exists as
         * this java object.
         */
        DRAFT(),
        /**
         * The certificate has been signed. It is stored in the database.
         * {@link Certificate#cert()} is valid.
         */
        ISSUED(),

        /**
         * The certificate has been revoked.
         */
        REVOKED(),

        /**
         * If this certificate cannot be updated because an error happened in
         * the signer.
         */
        ERROR();

        private CertificateStatus() {}

    }

    public synchronized CertificateStatus getStatus() {
        if (id == 0) {
            return CertificateStatus.DRAFT;
        }
        GigiPreparedStatement searcher = DatabaseConnection.getInstance().prepare("SELECT crt_name, created, revoked, serial, caid FROM certs WHERE id=?");
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
    public Job issue(Date start, String period) throws IOException, GigiApiException {
        if (getStatus() != CertificateStatus.DRAFT) {
            throw new IllegalStateException();
        }
        Notary.writeUserAgreement(owner, "CCA", "issue certificate", "", true, 0);

        GigiPreparedStatement inserter = DatabaseConnection.getInstance().prepare("INSERT INTO certs SET md=?::`mdType`, csr_type=?::`csrType`, crt_name='', memid=?, profile=?");
        inserter.setString(1, md.toLowerCase());
        inserter.setString(2, csrType.toString());
        inserter.setInt(3, owner.getId());
        inserter.setInt(4, profile.getId());
        inserter.execute();
        id = inserter.lastInsertId();

        GigiPreparedStatement san = DatabaseConnection.getInstance().prepare("INSERT INTO `subjectAlternativeNames` SET `certId`=?, contents=?, type=?::`SANType`");
        for (SubjectAlternateName subjectAlternateName : sans) {
            san.setInt(1, id);
            san.setString(2, subjectAlternateName.getName());
            san.setString(3, subjectAlternateName.getType().getOpensslName());
            san.execute();
        }

        GigiPreparedStatement insertAVA = DatabaseConnection.getInstance().prepare("INSERT INTO `certAvas` SET certid=?, name=?, value=?");
        insertAVA.setInt(1, id);
        for (Entry<String, String> e : dn.entrySet()) {
            insertAVA.setString(2, e.getKey());
            insertAVA.setString(3, e.getValue());
            insertAVA.execute();
        }
        File csrFile = KeyStorage.locateCsr(id);
        csrName = csrFile.getPath();
        try (FileOutputStream fos = new FileOutputStream(csrFile)) {
            fos.write(csr.getBytes("UTF-8"));
        }

        GigiPreparedStatement updater = DatabaseConnection.getInstance().prepare("UPDATE `certs` SET `csr_name`=? WHERE id=?");
        updater.setString(1, csrName);
        updater.setInt(2, id);
        updater.execute();
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

    public String getMessageDigest() {
        return md;
    }

    public User getOwner() {
        return owner;
    }

    public List<SubjectAlternateName> getSANs() {
        return Collections.unmodifiableList(sans);
    }

    public CertificateProfile getProfile() {
        return profile;
    }

    public static Certificate getBySerial(String serial) {
        if (serial == null || "".equals(serial)) {
            return null;
        }
        // TODO caching?
        try {
            String concat = "string_agg(concat('/', `name`, '=', REPLACE(REPLACE(value, '\\\\', '\\\\\\\\'), '/', '\\\\/')), '')";
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT certs.id, " + concat + " as `subject`, `md`, `csr_name`, `crt_name`,`memid`, `profile`, `certs`.`serial` FROM `certs` LEFT JOIN `certAvas` ON `certAvas`.`certid`=`certs`.`id` WHERE `serial`=? GROUP BY `certs`.`id`");
            ps.setString(1, serial);
            GigiResultSet rs = ps.executeQuery();
            return new Certificate(rs);
        } catch (IllegalArgumentException e) {

        }
        return null;
    }

    public static Certificate getById(int id) {

        // TODO caching?
        try {
            String concat = "group_concat(concat('/', `name`, '=', REPLACE(REPLACE(value, '\\\\', '\\\\\\\\'), '/', '\\\\/')))";
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT certs.id, " + concat + " as subject, md, csr_name, crt_name,memid, profile, certs.serial FROM `certs` LEFT JOIN certAvas ON certAvas.certid=certs.id WHERE certs.id=? GROUP BY certs.id");
            ps.setInt(1, id);
            GigiResultSet rs = ps.executeQuery();

            return new Certificate(rs);
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
            GigiPreparedStatement prep = DatabaseConnection.getInstance().prepare("SELECT revoked FROM certs WHERE id=?");
            prep.setInt(1, getId());
            GigiResultSet res = prep.executeQuery();
            res.beforeFirst();
            if (res.next()) {
                return new java.util.Date(res.getDate("revoked").getTime());
            }
        }
        return null;
    }
}
