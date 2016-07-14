package org.cacert.gigi.dbObjects;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.email.SendMail;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.SprintfCommand;
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

    public boolean setName(Name newName) throws GigiApiException {
        if (newName.equals(target.getName())) {
            return false;
        }
        writeSELog("SE Name change");
        target.setName(newName);
        return true;
    }

    public boolean setDob(DayDate dob) throws GigiApiException {
        if (dob.equals(target.getDoB())) {
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

    public void sendSupportNotification(String subject, Outputable message) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter outMail = new PrintWriter(sw);
            outMail.print("Hi," + "\n\n");
            SprintfCommand.createSimple("supporter {0} triggered:", supporter.getName().toString()).output(outMail, Language.getInstance(Locale.ENGLISH), new HashMap<String, Object>());
            outMail.print("\n\n");
            message.output(outMail, Language.getInstance(Locale.ENGLISH), new HashMap<String, Object>());
            outMail.print("\n\n");
            outMail.print("RA DB");
            outMail.close();
            String supportemailaddress = "support@" + ServerConstants.getWwwHostName().replaceFirst("^www\\.", "");
            SendMail.getInstance().sendMail(supportemailaddress, "[" + this.getTicket() + "] RA DB " + subject, sw.toString(), supportemailaddress, null, null, null, null, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
