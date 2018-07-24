package club.wpia.gigi.pages;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.CATS.CATSType;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Country.CountryCodeType;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;

public class TestMain extends ClientTest {

    private User orgAdmin;

    @Test
    public void testPasswordLogin() throws MalformedURLException, IOException, GigiApiException {
        URLConnection uc = new URL("https://" + getServerName()).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String content = IOUtils.readURL(uc);

        assertThat(content, not(containsString("via certificate")));

        makeAgent(u.getId());
        uc = new URL("https://" + getServerName()).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        content = IOUtils.readURL(uc);
        assertThat(content, containsString("For some actions, e.g. add verification, support, you need to be authenticated via certificate."));

    }

    @Test
    public void testCertLogin() throws GeneralSecurityException, IOException, GigiApiException, InterruptedException {
        cookie = cookieWithCertificateLogin(u);

        URLConnection uc = new URL("https://" + getSecureServerName()).openConnection();
        authenticate((HttpURLConnection) uc);
        String content = IOUtils.readURL(uc);
        assertThat(content, not(containsString("via certificate")));

        makeAgent(u.getId());
        uc = new URL("https://" + getSecureServerName()).openConnection();
        authenticate((HttpURLConnection) uc);
        content = IOUtils.readURL(uc);
        assertThat(content, containsString("You are authenticated via certificate, so you will be able to perform all actions."));
    }

    @Test
    public void testPasswordLoginOrgAdmin() throws MalformedURLException, IOException, GigiApiException {
        URLConnection uc = new URL("https://" + getServerName()).openConnection();
        addOrgAdmin();
        cookie = login(orgAdmin.getEmail(), TEST_PASSWORD);
        loginCertificate = null;
        uc.addRequestProperty("Cookie", cookie);
        String content = IOUtils.readURL(uc);
        assertThat(content, containsString("You need to be logged in via certificate to get access to the organisations."));
        assertThat(content, containsString("For some actions, e.g. add verification, support, you need to be authenticated via certificate."));

    }

    @Test
    public void testCertLoginOrgAdmin() throws GeneralSecurityException, IOException, GigiApiException, InterruptedException {
        cookie = cookieWithCertificateLogin(u);
        addOrgAdmin();
        cookie = cookieWithCertificateLogin(orgAdmin);

        URLConnection uc = new URL("https://" + getSecureServerName()).openConnection();
        authenticate((HttpURLConnection) uc);
        String content = IOUtils.readURL(uc);

        assertThat(content, containsString("change to organisation administrator context"));
        assertThat(content, containsString("You are authenticated via certificate, so you will be able to perform all actions."));
    }

    private void addOrgAdmin() throws GigiApiException, IOException {
        makeAgent(u.getId());
        u.grantGroup(getSupporter(), Group.ORG_AGENT);
        clearCaches();
        Organisation o = new Organisation(createUniqueName(), Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS), "pr", "city", "test@example.com", "", "", u);
        orgAdmin = User.getById(createVerificationUser("testworker", "testname", createUniqueName() + "@testdom.com", TEST_PASSWORD));
        makeAgent(orgAdmin.getId());
        o.addAdmin(orgAdmin, u, true);
    }

    @Test
    public void testValidChallenges() throws GeneralSecurityException, IOException, GigiApiException, InterruptedException {
        insertRAContract(u.getId());
        // test RA Agent challenge
        cookie = cookieWithCertificateLogin(u);

        testChallengeText("you need to pass the RA Agent Challenge", false);

        add100Points(u.getId());
        addChallengeInPast(u.getId(), CATSType.AGENT_CHALLENGE);
        testChallengeText("you need to pass the RA Agent Challenge", true);

        addChallenge(u.getId(), CATSType.AGENT_CHALLENGE);
        testChallengeText("you need to pass the RA Agent Challenge", false);

        // test Support challenge
        testChallengeText("you need to pass the Support Challenge", false);

        grant(u, Group.SUPPORTER);
        cookie = login(loginPrivateKey, loginCertificate.cert());
        testChallengeText("you need to pass the Support Challenge", true);

        addChallengeInPast(u.getId(), CATSType.SUPPORT_DP_CHALLENGE_NAME);
        testChallengeText("you need to pass the Support Challenge", true);

        addChallenge(u.getId(), CATSType.SUPPORT_DP_CHALLENGE_NAME);
        testChallengeText("you need to pass the Support Challenge", false);

        // test Org Agent challenge
        testChallengeText("you need to pass the Organisation Agent Challenge", false);

        grant(u, Group.ORG_AGENT);
        cookie = login(loginPrivateKey, loginCertificate.cert());
        testChallengeText("you need to pass the Organisation Agent Challenge", true);

        addChallengeInPast(u.getId(), CATSType.ORG_AGENT_CHALLENGE);
        testChallengeText("you need to pass the Organisation Agent Challenge", true);

        addChallenge(u.getId(), CATSType.ORG_AGENT_CHALLENGE);
        testChallengeText("you need to pass the Organisation Agent Challenge", false);

        // test TTP Agent challenge
        testChallengeText("you need to pass the TTP RA Agent Challenge", false);

        grant(u, Group.TTP_AGENT);
        cookie = login(loginPrivateKey, loginCertificate.cert());
        testChallengeText("you need to pass the TTP RA Agent Challenge", true);

        addChallengeInPast(u.getId(), CATSType.TTP_AGENT_CHALLENGE);
        testChallengeText("you need to pass the TTP RA Agent Challenge", true);

        addChallenge(u.getId(), CATSType.TTP_AGENT_CHALLENGE);
        testChallengeText("you need to pass the TTP RA Agent Challenge", false);

        // test Org Admin Challenge
        Organisation o = new Organisation(createUniqueName(), Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS), "pr", "city", "test@example.com", "", "", u);
        User admin = User.getById(createVerificationUser("testworker", "testname", createUniqueName() + "@testdom.com", TEST_PASSWORD));

        loginCertificate = null;
        cookie = cookieWithCertificateLogin(admin);
        testChallengeText("you need to pass the Organisation Administrator Challenge", false);

        o.addAdmin(admin, u, true);
        testChallengeText("you need to pass the Organisation Administrator Challenge", true);

        addChallengeInPast(admin.getId(), CATSType.ORG_ADMIN_DP_CHALLENGE_NAME);
        testChallengeText("you need to pass the Organisation Administrator Challenge", true);

        addChallenge(admin.getId(), CATSType.ORG_ADMIN_DP_CHALLENGE_NAME);
        testChallengeText("you need to pass the Organisation Administrator Challenge", false);
    }

    private void testChallengeText(String contentText, boolean contains) throws IOException, MalformedURLException, GigiApiException {
        URLConnection uc = new URL("https://" + getSecureServerName()).openConnection();
        authenticate((HttpURLConnection) uc);
        String content = IOUtils.readURL(uc);
        if (contains) {
            assertThat(content, containsString(contentText));
        } else {
            assertThat(content, not(containsString(contentText)));
        }
    }
}
