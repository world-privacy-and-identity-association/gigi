package org.cacert.gigi.pages.admin;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.pages.admin.support.SupportEnterTicketPage;
import org.cacert.gigi.pages.admin.support.SupportUserDetailsPage;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.TestEmailReceiver.TestMail;
import org.cacert.gigi.util.ServerConstants;
import org.junit.Test;

public class TestSEAdminNotificationMail extends ClientTest {

    private int targetID;

    public TestSEAdminNotificationMail() throws IOException {
        grant(email, Group.SUPPORTER);
        assertEquals(302, post(cookie, SupportEnterTicketPage.PATH, "ticketno=a20140808.8&setTicket=action", 0).getResponseCode());

        String email = createUniqueName() + "@example.com";
        String fname = "Först";
        String lname = "Secönd";
        targetID = createVerifiedUser(fname, lname, email, TEST_PASSWORD);
    }

    @Test
    public void testChangeAccountData() throws MalformedURLException, IOException {

        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID, "dobd=1&dobm=2&doby=2000&detailupdate", 0);

        String message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("The account data was changed."));
        assertThat(message, containsString("supporter " + u.getPreferredName().toString() + " triggered:"));

    }

    @Test
    public void testPasswordReset() throws MalformedURLException, IOException {
        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID, "aword=SecretWord&resetPass", 0);
        TestMail tm;
        String targetMail = ServerConstants.getSupportMailAddress();
        do {
            tm = getMailReceiver().receive();
        } while ( !tm.getTo().equals(targetMail));
        assertThat(tm.getMessage(), containsString("A password reset was triggered and an email was sent to user."));
    }

    @Test
    public void testGrantUserGroup() throws MalformedURLException, IOException {
        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID, "grant&groupToModify=supporter", 0);

        String message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("The group permission supporter was granted."));
    }

    @Test
    public void testRemoveUserGroup() throws MalformedURLException, IOException {
        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID, "deny&groupToModify=supporter", 0);

        String message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("The group permission supporter was revoked."));
    }

    @Test
    public void testRevokeCertificates() throws MalformedURLException, IOException {
        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID, "revokeall", 1);

        String message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("All certificates in the account have been revoked."));

    }
}
