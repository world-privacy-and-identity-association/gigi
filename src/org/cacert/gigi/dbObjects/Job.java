package org.cacert.gigi.dbObjects;

import java.sql.Date;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DBEnum;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.CertificateValiditySelector;

public class Job implements IdCachable {

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

    public synchronized static Job sign(Certificate targetId, Date start, String period) throws GigiApiException {
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

    public synchronized static Job revoke(Certificate targetId) {

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
