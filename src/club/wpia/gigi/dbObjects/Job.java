package club.wpia.gigi.dbObjects;

import java.sql.Date;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.DBEnum;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.dbObjects.Certificate.RevocationType;
import club.wpia.gigi.output.CertificateValiditySelector;

public class Job implements IdCachable {

    public static int WAIT_MIN = 60000;

    private int id;

    private Job(int id) {
        this.id = id;
    }

    public static enum JobType implements DBEnum {
        SIGN("sign"), REVOKE("revoke");

        private final String name;

        private JobType(String name) {
            this.name = name;
        }

        @Override
        public String getDBName() {
            return name;
        }
    }

    protected synchronized static Job sign(Certificate targetId, Date start, String period) throws GigiApiException {
        CertificateValiditySelector.checkValidityLength(period);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `jobs` SET targetId=?, task=?::`jobType`, executeFrom=?, executeTo=?")) {
            ps.setInt(1, targetId.getId());
            ps.setEnum(2, JobType.SIGN);
            ps.setDate(3, start);
            ps.setString(4, period);
            ps.execute();
            return cache.put(new Job(ps.lastInsertId()));
        }
    }

    protected synchronized static Job revoke(Certificate targetId, RevocationType type) {
        return revoke(targetId, type, null, null, null);
    }

    protected synchronized static Job revoke(Certificate targetId, String challenge, String signature, String message) {
        return revoke(targetId, RevocationType.KEY_COMPROMISE, challenge, signature, message);
    }

    private synchronized static Job revoke(Certificate targetId, RevocationType type, String challenge, String signature, String message) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `certs` SET `revocationType`=?::`revocationType`, `revocationChallenge`=?, `revocationSignature`=?, `revocationMessage`=? WHERE id=?")) {
            ps.setEnum(1, type);
            ps.setString(2, challenge);
            ps.setString(3, signature);
            ps.setString(4, message);
            ps.setInt(5, targetId.getId());
            ps.execute();
        }

        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `jobs` SET targetId=?, task=?::`jobType`")) {
            ps.setInt(1, targetId.getId());
            ps.setEnum(2, JobType.REVOKE);
            ps.execute();
            return cache.put(new Job(ps.lastInsertId()));
        }
    }

    public synchronized boolean waitFor(int max) {
        long start = System.currentTimeMillis();
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT 1 FROM `jobs` WHERE id=? AND state='open'")) {
            ps.setInt(1, id);
            GigiResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rs.close();
                if (max != 0 && System.currentTimeMillis() - start > max) {
                    return false;
                }
                try {
                    this.wait((long) (2000 + Math.random() * 2000));
                } catch (InterruptedException ie) {
                    // Ignore the interruption
                    ie.printStackTrace();
                }
                rs = ps.executeQuery();
            }
        }
        return true;
    }

    @Override
    public int getId() {
        return id;
    }

    static ObjectCache<Job> cache = new ObjectCache<>();

    public synchronized static Job getById(int id) {
        Job i = cache.get(id);
        if (i != null) {
            return i;
        }
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT 1 FROM `jobs` WHERE id=?")) {
            ps.setInt(1, id);
            GigiResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Job j = new Job(id);
                cache.put(j);
                return j;
            }
            return null;
        }

    }
}
