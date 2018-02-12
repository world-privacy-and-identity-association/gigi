package club.wpia.gigi.pages.admin;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.pages.account.MyDetails;
import club.wpia.gigi.pages.admin.support.SupportUserDetailsPage;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.SEClientTest;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class TestSEAdminNotificationMail extends SEClientTest {

    private int targetID;

    private String targetEmail;

    public TestSEAdminNotificationMail() throws IOException, GigiApiException {
        targetEmail = createUniqueName() + "@example.com";
        String fname = "Först";
        String lname = "Secönd";
        targetID = createVerifiedUser(fname, lname, targetEmail, TEST_PASSWORD);
    }

    @Test
    public void testChangeAccountData() throws MalformedURLException, IOException {

        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID + "/", "dobd=1&dobm=2&doby=2000&detailupdate", 0);

        // mail to support
        String message = getMailReceiver().receive(ServerConstants.getSupportMailAddress()).getMessage();
        assertThat(message, containsString("The DoB was changed"));
        assertThat(message, containsString("supporter " + u.getPreferredName().toString() + " triggered:"));
        // mail to user
        message = getMailReceiver().receive(targetEmail).getMessage();
        assertThat(message, containsString("The DoB in your account was changed to 2000-02-01."));
    }

    @Test
    public void testPasswordReset() throws MalformedURLException, IOException {
        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID + "/", "aword=SecretWord&resetPass", 0);
        getMailReceiver().receive(targetEmail);
        TestMail tm = getMailReceiver().receive(ServerConstants.getSupportMailAddress());
        assertThat(tm.getMessage(), containsString("A password reset was triggered and an email was sent to user."));
    }

    @Test
    public void testGrantUserGroup() throws MalformedURLException, IOException {
        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID + "/", "addGroup&groupToModify=" + URLEncoder.encode(Group.CODESIGNING.getDBName(), "UTF-8"), 0);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Group.CODESIGNING.getName().output(pw, Language.getInstance(Locale.ENGLISH), new HashMap<String, Object>());

        // mail to support
        String message = getMailReceiver().receive(ServerConstants.getSupportMailAddress()).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was granted."));
        // mail to user
        message = getMailReceiver().receive(targetEmail).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was granted to your account."));
    }

    @Test
    public void testRemoveUserGroup() throws MalformedURLException, IOException {
        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID + "/", "removeGroup&groupToModify=" + URLEncoder.encode(Group.CODESIGNING.getDBName(), "UTF-8"), 0);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Group.CODESIGNING.getName().output(pw, Language.getInstance(Locale.ENGLISH), new HashMap<String, Object>());

        // mail to support
        String message = getMailReceiver().receive(ServerConstants.getSupportMailAddress()).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was revoked."));
        // mail to user
        message = getMailReceiver().receive(targetEmail).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was revoked from your account."));
    }

    @Test
    public void testGrantSupporterGroup() throws MalformedURLException, IOException {
        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID + "/", "addGroup&groupToModify=" + URLEncoder.encode(Group.SUPPORTER.getDBName(), "UTF-8"), 0);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Group.SUPPORTER.getName().output(pw, Language.getInstance(Locale.ENGLISH), new HashMap<String, Object>());
        User target = User.getById(targetID);

        // mail to support
        String message = getMailReceiver().receive(ServerConstants.getSupportMailAddress()).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was granted."));
        // mail to user
        message = getMailReceiver().receive(targetEmail).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was granted to your account."));
        // mail to board
        message = getMailReceiver().receive(ServerConstants.getBoardMailAddress()).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was granted for '" + target.getPreferredName().toString() + "'."));
    }

    @Test
    public void testRemoveSupporterGroup() throws MalformedURLException, IOException {
        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID + "/", "removeGroup&groupToModify=" + URLEncoder.encode(Group.SUPPORTER.getDBName(), "UTF-8"), 0);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Group.SUPPORTER.getName().output(pw, Language.getInstance(Locale.ENGLISH), new HashMap<String, Object>());
        User target = User.getById(targetID);

        // mail to support
        String message = getMailReceiver().receive(ServerConstants.getSupportMailAddress()).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was revoked."));
        // mail to user
        message = getMailReceiver().receive(targetEmail).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was revoked from your account."));
        // mail to board
        message = getMailReceiver().receive(ServerConstants.getBoardMailAddress()).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was revoked for '" + target.getPreferredName().toString() + "'."));
    }

    @Test
    public void testRevokeAllCertificates() throws MalformedURLException, IOException {
        executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + targetID + "/", "revokeall", 1);
        User user = User.getById(targetID);

        // mail to support
        String message = getMailReceiver().receive(ServerConstants.getSupportMailAddress()).getMessage();
        assertThat(message, containsString("All certificates in the account " + user.getPreferredName().toString()));
        // mail to user
        message = getMailReceiver().receive(targetEmail).getMessage();
        assertThat(message, containsString("All certificates in your account have been revoked."));
    }

    @Test
    public void testSupportSupporterGroup() throws MalformedURLException, IOException {
        // supporter adds to his own groups
        String s = IOUtils.readURL(post(SupportUserDetailsPage.PATH + u.getId() + "/", "addGroup&groupToModify=" + URLEncoder.encode(Group.ORG_AGENT.getDBName(), "UTF-8")));
        assertThat(s, containsString("Supporter may not modify himself."));

        // supporter removes from his own groups
        s = IOUtils.readURL(post(SupportUserDetailsPage.PATH + u.getId() + "/", "removeGroup&groupToModify=" + URLEncoder.encode(Group.ORG_AGENT.getDBName(), "UTF-8")));
        assertThat(s, containsString("Supporter may not modify himself."));

        // supporter removes supporter flag
        URLConnection uc = post(SupportUserDetailsPage.PATH + u.getId() + "/", "removeGroup&groupToModify=" + URLEncoder.encode(Group.SUPPORTER.getDBName(), "UTF-8"));
        assertEquals("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + MyDetails.PATH, uc.getHeaderField("Location"));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Group.SUPPORTER.getName().output(pw, Language.getInstance(Locale.ENGLISH), new HashMap<String, Object>());
        // mail to support
        String message = getMailReceiver().receive(ServerConstants.getSupportMailAddress()).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was revoked."));
        // mail to user
        message = getMailReceiver().receive(u.getEmail()).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was revoked from your account."));
        // mail to board
        message = getMailReceiver().receive(ServerConstants.getBoardMailAddress()).getMessage();
        assertThat(message, containsString("The group permission '" + sw.toString() + "' was revoked for '" + u.getPreferredName().toString() + "'."));
        s = IOUtils.readURL(get(cookie, MyDetails.PATH));
        assertThat(s, not(containsString("supporter")));
    }

}
