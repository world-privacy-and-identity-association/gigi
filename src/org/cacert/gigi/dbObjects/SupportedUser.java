package org.cacert.gigi.dbObjects;

import java.sql.Date;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;

public class SupportedUser {

    private User target;

    private User supporter;

    private String ticket;

    public SupportedUser(User target, User supporter, String ticket) {
        this.supporter = supporter;
        this.target = target;
        this.ticket = ticket;
    }

    public boolean setName(Name newName) throws GigiApiException {
        if (newName.equals(target.getName())) {
            return false;
        }
        writeSELog("SE Name change");
        target.setName(newName);
        return true;
    }

    public boolean setDob(Date dob) throws GigiApiException {
        if (dob.toString().equals(target.getDoB().toString())) {
            return false;
        }
        writeSELog("SE dob change");
        target.setDoB(dob);
        return true;
    }

    public void revokeAllCertificates() throws GigiApiException {
        writeSELog("SE Revoke certificates");
        Certificate[] certs = target.getCertificates(false);
        // TODO Check for open jobs!
        for (int i = 0; i < certs.length; i++) {
            if (certs[i].getStatus() == CertificateStatus.ISSUED) {
                certs[i].revoke();
            }
        }
    }

    private void writeSELog(String type) throws GigiApiException {
        if (ticket == null) {
            throw new GigiApiException("No ticket set!");
        }
        try (GigiPreparedStatement prep = new GigiPreparedStatement("INSERT INTO `adminLog` SET uid=?, admin=?, type=?, information=?")) {
            prep.setInt(1, target.getId());
            prep.setInt(2, supporter.getId());
            prep.setString(3, type);
            prep.setString(4, ticket);
            prep.executeUpdate();
        }
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

    public User getTargetUser() {
        return target;
    }

    public void submitSupportAction() throws GigiApiException {
        target.rawUpdateUserData();
    }

    public void grant(Group toMod) {
        target.grantGroup(supporter, toMod);
    }

    public void revoke(Group toMod) {
        target.revokeGroup(supporter, toMod);
    }

}
