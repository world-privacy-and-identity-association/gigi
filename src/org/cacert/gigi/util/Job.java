package org.cacert.gigi.util;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.output.CertificateValiditySelector;

public class Job {

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

    public static Job sign(Certificate targetId, Date start, String period) throws SQLException, GigiApiException {
        CertificateValiditySelector.checkValidityLength(period);
        PreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `jobs` SET targetId=?, task=?, executeFrom=?, executeTo=?");
        ps.setInt(1, targetId.getId());
        ps.setString(2, JobType.SIGN.getName());
        ps.setDate(3, start);
        ps.setString(4, period);
        ps.execute();
        return new Job(DatabaseConnection.lastInsertId(ps));
    }

    public static Job revoke(Certificate targetId) throws SQLException {

        PreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `jobs` SET targetId=?, task=?");
        ps.setInt(1, targetId.getId());
        ps.setString(2, JobType.REVOKE.getName());
        ps.execute();
        return new Job(DatabaseConnection.lastInsertId(ps));
    }

    public boolean waitFor(int max) throws SQLException, InterruptedException {
        long start = System.currentTimeMillis();
        PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT 1 FROM `jobs` WHERE id=? AND state='open'");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
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
}
