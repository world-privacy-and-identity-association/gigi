package org.cacert.gigi.dbObjects;

import java.io.IOException;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.email.MailProbe;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.RandomToken;

public class EmailAddress implements IdCachable {

    private String address;

    private int id;

    private User owner;

    private String hash = null;

    private EmailAddress(int id) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT memid, email, hash FROM `emails` WHERE id=? AND deleted=0");
        ps.setInt(1, id);

        GigiResultSet rs = ps.executeQuery();
        if ( !rs.next()) {
            throw new IllegalArgumentException("Invalid email id " + id);
        }
        this.id = id;
        owner = User.getById(rs.getInt(1));
        address = rs.getString(2);
        hash = rs.getString(3);
        rs.close();
    }

    public EmailAddress(User owner, String address) {
        if ( !EmailProvider.MAIL.matcher(address).matches()) {
            throw new IllegalArgumentException("Invalid email.");
        }
        this.address = address;
        this.owner = owner;
        this.hash = RandomToken.generateToken(16);
    }

    public void insert(Language l) {
        if (id != 0) {
            throw new IllegalStateException("already inserted.");
        }
        try {
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `emails` SET memid=?, hash=?, email=?");
            ps.setInt(1, owner.getId());
            ps.setString(2, hash);
            ps.setString(3, address);
            synchronized (EmailAddress.class) {
                ps.execute();
                id = ps.lastInsertId();
                myCache.put(this);
            }
            MailProbe.sendMailProbe(l, "email", id, hash, address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public synchronized void verify(String hash) throws GigiApiException {
        if (this.hash.equals(hash)) {
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE `emails` SET hash='' WHERE id=?");
            ps.setInt(1, id);
            ps.execute();
            hash = "";

            // Verify user with that primary email
            GigiPreparedStatement ps2 = DatabaseConnection.getInstance().prepare("update `users` set `verified`='1' where `id`=? and `email`=? and `verified`='0'");
            ps2.setInt(1, owner.getId());
            ps2.setString(2, address);
            ps2.execute();
            this.hash = "";

        } else {
            throw new GigiApiException("Email verification hash is invalid.");
        }
    }

    public boolean isVerified() {
        return hash.isEmpty();
    }

    private static ObjectCache<EmailAddress> myCache = new ObjectCache<>();

    public static synchronized EmailAddress getById(int id) throws IllegalArgumentException {
        EmailAddress em = myCache.get(id);
        if (em == null) {
            myCache.put(em = new EmailAddress(id));
        }
        return em;
    }
}
