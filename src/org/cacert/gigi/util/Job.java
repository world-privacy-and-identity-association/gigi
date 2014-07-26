package org.cacert.gigi.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cacert.gigi.Certificate;
import org.cacert.gigi.database.DatabaseConnection;

public class Job {

    int id;

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

    public static Job submit(Certificate targetId, JobType type) throws SQLException {
        PreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `jobs` SET targetId=?, task=?");
        ps.setInt(1, targetId.getId());
        ps.setString(2, type.getName());
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
