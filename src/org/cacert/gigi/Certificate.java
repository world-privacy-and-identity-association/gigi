package org.cacert.gigi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.util.Job;
import org.cacert.gigi.util.Job.JobType;
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

    private int ownerId;

    private String serial;

    private String dn;

    private String md;

    private String csrName;

    private String crtName;

    private String csr = null;

    private CSRType csrType;

    private List<SubjectAlternateName> sans;

    private CertificateProfile profile;

    public Certificate(int ownerId, String dn, String md, String csr, CSRType csrType, CertificateProfile profile, SubjectAlternateName... sans) {
        this.ownerId = ownerId;
        this.dn = dn;
        this.md = md;
        this.csr = csr;
        this.csrType = csrType;
        this.profile = profile;
        this.sans = Arrays.asList(sans);
    }

    private Certificate(String serial) {
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id,subject, md, csr_name, crt_name,memid, profile FROM `certs` WHERE serial=?");
            ps.setString(1, serial);
            ResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                throw new IllegalArgumentException("Invalid mid " + serial);
            }
            this.id = rs.getInt(1);
            dn = rs.getString(2);
            md = rs.getString(3);
            csrName = rs.getString(4);
            crtName = rs.getString(5);
            ownerId = rs.getInt(6);
            profile = CertificateProfile.getById(rs.getInt(7));
            this.serial = serial;

            PreparedStatement ps2 = DatabaseConnection.getInstance().prepare("SELECT contents, type FROM `subjectAlternativeNames` WHERE certId=?");
            ps2.setInt(1, id);
            ResultSet rs2 = ps2.executeQuery();
            sans = new LinkedList<>();
            while (rs2.next()) {
                sans.add(new SubjectAlternateName(SANType.valueOf(rs2.getString("type").toUpperCase()), rs2.getString("contents")));
            }
            rs2.close();

            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    public CertificateStatus getStatus() throws SQLException {
        if (id == 0) {
            return CertificateStatus.DRAFT;
        }
        PreparedStatement searcher = DatabaseConnection.getInstance().prepare("SELECT crt_name, created, revoked, serial FROM certs WHERE id=?");
        searcher.setInt(1, id);
        ResultSet rs = searcher.executeQuery();
        if ( !rs.next()) {
            throw new IllegalStateException("Certificate not in Database");
        }

        crtName = rs.getString(1);
        serial = rs.getString(4);
        if (rs.getTime(2) == null) {
            return CertificateStatus.DRAFT;
        }
        if (rs.getTime(2) != null && rs.getTime(3) == null) {
            return CertificateStatus.ISSUED;
        }
        return CertificateStatus.REVOKED;
    }

    public Job issue() throws IOException, SQLException {
        if (getStatus() != CertificateStatus.DRAFT) {
            throw new IllegalStateException();
        }
        Notary.writeUserAgreement(ownerId, "CCA", "issue certificate", "", true, 0);

        PreparedStatement inserter = DatabaseConnection.getInstance().prepare("INSERT INTO certs SET md=?, subject=?, csr_type=?, crt_name='', memid=?, profile=?");
        inserter.setString(1, md);
        inserter.setString(2, dn);
        inserter.setString(3, csrType.toString());
        inserter.setInt(4, ownerId);
        inserter.setInt(5, profile.getId());
        inserter.execute();
        id = DatabaseConnection.lastInsertId(inserter);
        File csrFile = KeyStorage.locateCsr(id);
        csrName = csrFile.getPath();
        FileOutputStream fos = new FileOutputStream(csrFile);
        fos.write(csr.getBytes());
        fos.close();

        // TODO draft to insert SANs
        PreparedStatement san = DatabaseConnection.getInstance().prepare("INSERT INTO subjectAlternativeNames SET certId=?, contents=?, type=?");
        for (SubjectAlternateName subjectAlternateName : sans) {
            san.setInt(1, id);
            san.setString(2, subjectAlternateName.getName());
            san.setString(3, subjectAlternateName.getType().getOpensslName());
            san.execute();
        }

        PreparedStatement updater = DatabaseConnection.getInstance().prepare("UPDATE certs SET csr_name=? WHERE id=?");
        updater.setString(1, csrName);
        updater.setInt(2, id);
        updater.execute();
        return Job.submit(this, JobType.SIGN);

    }

    public Job revoke() throws SQLException {
        if (getStatus() != CertificateStatus.ISSUED) {
            throw new IllegalStateException();
        }
        return Job.submit(this, JobType.REVOKE);

    }

    public X509Certificate cert() throws IOException, GeneralSecurityException, SQLException {
        CertificateStatus status = getStatus();
        if (status != CertificateStatus.ISSUED) {
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
        try {
            getStatus();
        } catch (SQLException e) {
            e.printStackTrace();
        } // poll changes
        return serial;
    }

    public String getDistinguishedName() {
        return dn;
    }

    public String getMessageDigest() {
        return md;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public List<SubjectAlternateName> getSANs() {
        return Collections.unmodifiableList(sans);
    }

    public CertificateProfile getProfile() {
        return profile;
    }

    public static Certificate getBySerial(String serial) {
        // TODO caching?
        try {
            return new Certificate(serial);
        } catch (IllegalArgumentException e) {

        }
        return null;
    }

}
