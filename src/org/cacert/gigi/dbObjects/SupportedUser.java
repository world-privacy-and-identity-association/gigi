package org.cacert.gigi.dbObjects;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.MailTemplate;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.SprintfCommand;
import org.cacert.gigi.output.template.TranslateCommand;
import org.cacert.gigi.pages.PasswordResetPage;
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
        String subject = "Change DoB Data";
        // send notification to support
        Outputable message = new TranslateCommand("The DoB was changed.");
        sendSupportNotification(subject, message);
        // send notification to user
        message = SprintfCommand.createSimple("The DoB in your account was changed to {0}.", dob);
        sendSupportUserNotification(subject, message);
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
        // send notification to support
        Outputable message = SprintfCommand.createSimple("All certificates in the account {0} <{1}> have been revoked.", target.getPreferredName().toString(), target.getEmail());
        sendSupportNotification("Revoke certificates", message);
        // send notification to user
        sendSupportUserNotification("Revoke certificate", new TranslateCommand("All certificates in your account have been revoked."));
    }

    public void revokeCertificate(Certificate cert) throws GigiApiException {

        // TODO Check for open jobs!
        if (cert.getStatus() == CertificateStatus.ISSUED) {
            writeSELog("SE Revoke certificate");
            cert.revoke().waitFor(60000);
            // send notification to support
            String subject = "Revoke certificate";
            Outputable message = SprintfCommand.createSimple("Certificate with serial number {0} for {1} <{2}> has been revoked.", cert.getSerial(), target.getPreferredName().toString(), target.getEmail());
            sendSupportNotification(subject, message);
            // send notification to user
            subject = "Revoke certificate";
            message = SprintfCommand.createSimple("Certificate with serial number {0} with subject distinguished name {1} has been revoked.", cert.getSerial(), cert.getDistinguishedName());
            sendSupportUserNotification(subject, message);
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
        String subject = "Change Group Permissions";
        // send notification to support
        Outputable message = SprintfCommand.createSimple("The group permission '{0}' was granted.", toMod.getName());
        sendSupportNotification(subject, message);
        // send notification to user
        message = SprintfCommand.createSimple("The group permission '{0}' was granted to your account.", toMod.getName());
        sendSupportUserNotification(subject, message);
    }

    public void revoke(Group toMod) {
        target.revokeGroup(supporter, toMod);
        String subject = "Change Group Permissions";
        // send notification to support
        Outputable message = SprintfCommand.createSimple("The group permission '{0}' was revoked.", toMod.getName());
        sendSupportNotification(subject, message);
        // send notification to user
        message = SprintfCommand.createSimple("The group permission '{0}' was revoked from your account.", toMod.getName());
        sendSupportUserNotification(subject, message);
    }

    private static final MailTemplate supportNotification = new MailTemplate(SupportedUser.class.getResource("SupportNotificationMail.templ"));

    private void sendSupportNotification(String subject, Outputable message) {
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

    private static final MailTemplate supportUserNotification = new MailTemplate(SupportedUser.class.getResource("SupportUserNotificationMail.templ"));

    private void sendSupportUserNotification(String subject, Outputable message) {
        try {
            HashMap<String, Object> vars = new HashMap<>();
            vars.put("action", message);
            vars.put("ticket", this.getTicket());
            vars.put("subject", subject);

            supportUserNotification.sendMail(Language.getInstance(Locale.ENGLISH), vars, target.getEmail());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void triggerPasswordReset(String aword, PrintWriter out, HttpServletRequest req) {
        Language l = Language.getInstance(target.getPreferredLocale());
        String method = l.getTranslation("A password reset was triggered. Please enter the required text sent to you by support on this page:");
        String subject = l.getTranslation("Password reset by support.");
        PasswordResetPage.initPasswordResetProcess(out, target, req, aword, l, method, subject);
        Outputable message = new TranslateCommand("A password reset was triggered and an email was sent to user.");
        sendSupportNotification(subject, message);
    }
}
