package org.cacert.gigi.dbObjects;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.MailTemplate;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.util.DayDate;
import org.cacert.gigi.util.ServerConstants;

public class SupportedUser {

    private User target;

    private User supporter;

    private String ticket;

    public SupportedUser(User target, User supporter, String ticket) {
        this.supporter = supporter;
        this.target = target;
        this.ticket = ticket;
    }

    public boolean setDob(DayDate dob) throws GigiApiException {
        if (dob.equals(target.getDoB())) {
            return false;
        }
        writeSELog("SE dob change");
        target.setDoBAsSupport(dob);
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

    public void revokeCertificate(Certificate cert) throws GigiApiException {

        // TODO Check for open jobs!
        if (cert.getStatus() == CertificateStatus.ISSUED) {
            writeSELog("SE Revoke certificate");
            cert.revoke().waitFor(60000);
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

    public void grant(Group toMod) throws GigiApiException {
        target.grantGroup(supporter, toMod);
    }

    public void revoke(Group toMod) {
        target.revokeGroup(supporter, toMod);
    }

    private static final MailTemplate supportNotification = new MailTemplate(SupportedUser.class.getResource("SupportNotificationMail.templ"));

    public void sendSupportNotification(String subject, Outputable message) {
        try {
            HashMap<String, Object> vars = new HashMap<>();
            vars.put("supporter", supporter.getPreferredName().toString());
            vars.put("action", message);
            vars.put("ticket", this.getTicket());
            vars.put("subject", subject);

            String supportemailaddress = ServerConstants.getSupportMailAddress();
            supportNotification.sendMail(Language.getInstance(Locale.ENGLISH), vars, supportemailaddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
