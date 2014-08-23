package org.cacert.gigi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cacert.gigi.database.DatabaseConnection;

public class Domain {

    private User owner;

    private String suffix;

    private int id;

    public Domain(int id) throws SQLException {
        PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT memid, domain FROM `domains` WHERE id=? AND deleted IS NULL");
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if ( !rs.next()) {
            throw new IllegalArgumentException("Invalid email id " + id);
        }
        this.id = id;
        owner = User.getById(rs.getInt(1));
        suffix = rs.getString(2);
        rs.close();
    }

    public Domain(User owner, String suffix) throws GigiApiException {
        this.owner = owner;
        this.suffix = suffix;

    }

    private static void checkInsert(String suffix) throws GigiApiException {
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT 1 FROM `domains` WHERE (domain=RIGHT(?,LENGTH(domain))  OR RIGHT(domain,LENGTH(?))=?) AND deleted IS NULL");
            ps.setString(1, suffix);
            ps.setString(2, suffix);
            ps.setString(3, suffix);
            ResultSet rs = ps.executeQuery();
            boolean existed = rs.next();
            rs.close();
            if (existed) {
                throw new GigiApiException("Domain could not be inserted. Domain is already valid.");
            }
        } catch (SQLException e) {
            throw new GigiApiException(e);
        }
    }

    public void insert() throws GigiApiException {
        if (id != 0) {
            throw new GigiApiException("already inserted.");
        }
        synchronized (Domain.class) {
            checkInsert(suffix);
            try {
                PreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `domains` SET memid=?, domain=?");
                ps.setInt(1, owner.getId());
                ps.setString(2, suffix);
                ps.execute();
                id = DatabaseConnection.lastInsertId(ps);
            } catch (SQLException e) {
                throw new GigiApiException(e);
            }
        }
    }

    public void delete() throws GigiApiException {
        if (id == 0) {
            throw new GigiApiException("not inserted.");
        }
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE `domains` SET deleted=CURRENT_TIMESTAMP WHERE id=?");
            ps.setInt(1, id);
            ps.execute();
        } catch (SQLException e) {
            throw new GigiApiException(e);
        }
    }

    public User getOwner() {
        return owner;
    }

    public int getId() {
        return id;
    }

    public String getSuffix() {
        return suffix;
    }

    public static Domain getById(int id) throws IllegalArgumentException {
        // TODO cache
        try {
            Domain e = new Domain(id);
            return e;
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
