package org.cacert.gigi.dbObjects;

import java.sql.Date;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.CertificateValiditySelector;

public class Job implements IdCachable {

    private int id;

    private Job(int id) {
        this.id = id;
    }

    public static enum JobType {
        SIGN("sign"), REVOKE("revoke");

        private final String name;

        private JobType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static Job sign(Certificate targetId, Date start, String period) throws GigiApiException {
        CertificateValiditySelector.checkValidityLength(period);
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `jobs` SET targetId=?, task=?::`jobType`, executeFrom=?, executeTo=?");
        ps.setInt(1, targetId.getId());
        ps.setString(2, JobType.SIGN.getName());
        ps.setDate(3, start);
        ps.setString(4, period);
        ps.execute();
        return cache.put(new Job(ps.lastInsertId()));
    }

    public synchronized static Job revoke(Certificate targetId) {

        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `jobs` SET targetId=?, task=?::`jobType`");
        ps.setInt(1, targetId.getId());
        ps.setString(2, JobType.REVOKE.getName());
        ps.execute();
        return cache.put(new Job(ps.lastInsertId()));
    }

    public synchronized boolean waitFor(int max) throws InterruptedException {
        long start = System.currentTimeMillis();
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT 1 FROM `jobs` WHERE id=? AND state='open'");
        ps.setInt(1, id);
        GigiResultSet rs = ps.executeQuery();
        while (rs.next()) {
            rs.close();
            if (max != 0 && System.currentTimeMillis() - start > max) {
                return false;
            }
            Thread.sleep((long) (2000 + Math.random() * 2000));
            rs = ps.executeQuery();
        }
        rs.close();
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
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT 1 FROM `jobs` WHERE id=?'");
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
