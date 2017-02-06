package org.cacert.gigi.dbObjects;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.email.MailProbe;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.SprintfCommand;
import org.cacert.gigi.util.RandomToken;

public class EmailAddress implements IdCachable, Verifyable {

    public static final int REPING_MINIMUM_DELAY = 5 * 60 * 1000;

    private String address;

    private int id;

    private User owner;

    private EmailAddress(int id) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `memid`, `email` FROM `emails` WHERE `id`=? AND `deleted` IS NULL")) {
            ps.setInt(1, id);

            GigiResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                throw new IllegalArgumentException("Invalid email id " + id);
            }
            this.id = id;
            owner = User.getById(rs.getInt(1));
            address = rs.getString(2);
        }
    }

    public EmailAddress(User owner, String address, Locale mailLocale) throws GigiApiException {
        address = address.toLowerCase();
        if ( !EmailProvider.isValidMailAddress(address)) {
            throw new IllegalArgumentException("Invalid email.");
        }
        this.address = address;
        this.owner = owner;
        insert(Language.getInstance(mailLocale));
    }

    private void insert(Language l) throws GigiApiException {
        try {
            synchronized (EmailAddress.class) {
                if (id != 0) {
                    throw new IllegalStateException("already inserted.");
                }
                try (GigiPreparedStatement psCheck = new GigiPreparedStatement("SELECT 1 FROM `emails` WHERE email=? AND deleted is NULL")) {
                    psCheck.setString(1, address);
                    GigiResultSet res = psCheck.executeQuery();
                    if (res.next()) {
                        throw new GigiApiException("The email address is already known to the system.");
                    }
                }
                try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `emails` SET memid=?, email=?")) {
                    ps.setInt(1, owner.getId());
                    ps.setString(2, address);
                    ps.execute();
                    id = ps.lastInsertId();
                }
                myCache.put(this);
            }
            ping(l);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ping(Language l) throws IOException {
        String hash = RandomToken.generateToken(16);
        try (GigiPreparedStatement statmt = new GigiPreparedStatement("INSERT INTO `emailPinglog` SET `when`=NOW(), `email`=?, `result`='', `uid`=?, `type`='active', `status`='open'::`pingState`, `challenge`=?")) {
            statmt.setString(1, address);
            statmt.setInt(2, owner.getId());
            statmt.setString(3, hash);
            statmt.execute();
        }

        MailProbe.sendMailProbe(l, "email", id, hash, address);
    }

    public int getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public synchronized boolean isVerifyable(String hash) throws GigiApiException {
        try (GigiPreparedStatement stmt = new GigiPreparedStatement("SELECT 1 FROM `emailPinglog` WHERE `email`=? AND `uid`=? AND `type`='active' AND `challenge`=? AND `status`='open'::`pingState`")) {
            stmt.setString(1, address);
            stmt.setInt(2, owner.getId());
            stmt.setString(3, hash);
            return stmt.executeQuery().next();
        }
    }

    public synchronized void verify(String hash) throws GigiApiException {
        try (GigiPreparedStatement stmt = new GigiPreparedStatement("UPDATE `emailPinglog` SET `status`='success'::`pingState` WHERE `email`=? AND `uid`=? AND `type`='active' AND `challenge`=? AND `status`='open'::`pingState`")) {
            stmt.setString(1, address);
            stmt.setInt(2, owner.getId());
            stmt.setString(3, hash);
            if ( !stmt.executeMaybeUpdate()) {
                throw new IllegalArgumentException("Given token could not be found to complete the verification process (Domain Ping).");
            }
        }
        // Verify user with that primary email
        try (GigiPreparedStatement ps2 = new GigiPreparedStatement("update `users` set `verified`='1' where `id`=? and `email`=? and `verified`='0'")) {
            ps2.setInt(1, owner.getId());
            ps2.setString(2, address);
            ps2.execute();
        }
    }

    public boolean isVerified() {
        try (GigiPreparedStatement statmt = new GigiPreparedStatement("SELECT 1 FROM `emailPinglog` WHERE `email`=? AND `uid`=? AND `type`='active' AND `status`='success'")) {
            statmt.setString(1, address);
            statmt.setInt(2, owner.getId());
            GigiResultSet e = statmt.executeQuery();
            return e.next();
        }
    }

    public Date getLastPing(boolean onlySuccess) {
        Date lastExecution;
        try (GigiPreparedStatement statmt = new GigiPreparedStatement("SELECT MAX(`when`) FROM `emailPinglog` WHERE `email`=? AND `uid`=? AND `type`='active'" + (onlySuccess ? " AND `status`='success'" : ""))) {
            statmt.setString(1, address);
            statmt.setInt(2, owner.getId());
            GigiResultSet e = statmt.executeQuery();
            if ( !e.next()) {
                return null;
            }
            lastExecution = e.getTimestamp(1);
        }
        return lastExecution;
    }

    public synchronized void requestReping(Language l) throws IOException, GigiApiException {
        Date lastExecution = getLastPing(false);

        if (lastExecution != null && lastExecution.getTime() + REPING_MINIMUM_DELAY >= System.currentTimeMillis()) {
            throw new GigiApiException(SprintfCommand.createSimple("Reping is only allowed after {0} minutes, yours end at {1}.", REPING_MINIMUM_DELAY / 60 / 1000, new Date(lastExecution.getTime() + REPING_MINIMUM_DELAY)));
        }
        ping(l);
        return;
    }

    private static ObjectCache<EmailAddress> myCache = new ObjectCache<>();

    public static synchronized EmailAddress getById(int id) throws IllegalArgumentException {
        EmailAddress em = myCache.get(id);
        if (em == null) {
            myCache.put(em = new EmailAddress(id));
        }
        return em;
    }

    public User getOwner() {
        return owner;
    }

    public static EmailAddress[] findByAllEmail(String mail) {
        LinkedList<EmailAddress> results = new LinkedList<EmailAddress>();
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `emails`.`id` FROM `emails` INNER JOIN `users` ON `users`.`id` = `emails`.`memid` INNER JOIN `certOwners` ON `certOwners`.`id` = `users`.`id` WHERE `emails`.`email` LIKE ? AND `emails`.`deleted` IS NULL AND `certOwners`.`deleted` IS NULL ORDER BY `users`.`id`, `emails`.`email` LIMIT 100")) {
            ps.setString(1, mail);
            GigiResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(EmailAddress.getById(rs.getInt(1)));
            }
            return results.toArray(new EmailAddress[results.size()]);
        }
    }
}
