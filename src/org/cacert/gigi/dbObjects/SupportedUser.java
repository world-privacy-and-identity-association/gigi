package org.cacert.gigi.dbObjects;

import java.sql.Date;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;

public class SupportedUser {

    private User target, supporter;

    private String ticket;

    public SupportedUser(User target, User supporter, String ticket) {
        this.supporter = supporter;
        this.target = target;
        this.ticket = ticket;
    }

    public void setName(String fname, String mname, String lname, String suffix) {
        writeSELog("SE Name change");
        target.setName(new Name(fname, lname, mname, suffix));
    }

    public void setDob(Date dob) {
        writeSELog("SE dob change");
        target.setDoB(dob);
    }

    public void revokeAllCertificates() {
        writeSELog("SE Revoke certificates");
        Certificate[] certs = target.getCertificates(false);
        for (int i = 0; i < certs.length; i++) {
            certs[i].revoke();
        }
    }

    public void writeSELog(String type) {
        GigiPreparedStatement prep = DatabaseConnection.getInstance().prepare("INSERT INTO adminLog SET uid=?, admin=?, type=?, information=?");
        prep.setInt(1, target.getId());
        prep.setInt(2, supporter.getId());
        prep.setString(3, type);
        prep.setString(4, ticket);
        prep.executeUpdate();
    }

    public int getId() {
        return target.getId();
    }

    public Certificate[] getCertificates(boolean includeRevoked) {
        return target.getCertificates(includeRevoked);
    }

    public String getTicket() {
        return ticket;
    }

}
