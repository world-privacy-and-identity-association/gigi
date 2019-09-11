package club.wpia.gigi.pages.orga;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.CATS.CATSType;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.OrgTest;

public class TestOrgSwitch extends OrgTest {

    private User u2;

    private Organisation org1 = createUniqueOrg();

    private Organisation org2 = createUniqueOrg();

    private Certificate cagent;

    private PrivateKey pkagent;

    public TestOrgSwitch() throws IOException, GigiApiException {

        assertEquals(403, get(SwitchOrganisation.PATH).getResponseCode());

        String email = createUniqueName() + "@testdom.com";
        u2 = User.getById(createVerificationUser("testworker", "testname", email, TEST_PASSWORD));
        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + org1.getId(), "email=" + URLEncoder.encode(u2.getEmail(), "UTF-8") + "&do_affiliate=y&master=y", 1));
        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + org2.getId(), "email=" + URLEncoder.encode(u2.getEmail(), "UTF-8") + "&do_affiliate=y&master=y", 1));
        addChallenge(u2.getId(), CATSType.ORG_ADMIN_DP_CHALLENGE_NAME);
        cagent = loginCertificate;
        pkagent = loginPrivateKey;

        // login with new user u2
        cookie = cookieWithCertificateLogin(u2);
    }

    @After
    public void purgeDbAfterTest() throws SQLException, IOException {
        purgeDatabase();
    }

    @Test
    public void testSwitchToOrg() throws IOException, GigiApiException {

        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org:" + org1.getId() + "=y", 0));

        String res = IOUtils.readURL(get(SwitchOrganisation.PATH));
        assertThat(res, containsString("Logged in as " + u2.getPreferredName() + ", acting as " + org1.getName()));

    }

    @Test
    public void testSwitchToNonOrg() throws IOException, GigiApiException {

        String res = IOUtils.readURL(post(SwitchOrganisation.PATH, "org:5000=y"));
        assertThat(res, containsString("Context switch failed"));

    }

    @Test
    public void testSwitchToPersonal() throws IOException, GigiApiException {

        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org-leave=personal", 0));

        String res = IOUtils.readURL(get(SwitchOrganisation.PATH));
        assertThat(res, containsString("Logged in as " + u2.getPreferredName()));

        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org-leave=personal", 0));

        res = IOUtils.readURL(get(SwitchOrganisation.PATH));
        assertThat(res, containsString("Logged in as " + u2.getPreferredName()));

    }

    @Test
    public void testSwitchOrgToOrg() throws IOException, GigiApiException {

        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org:" + org1.getId() + "=y", 0));
        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org:" + org2.getId() + "=y", 0));

        String res = IOUtils.readURL(get(SwitchOrganisation.PATH));
        assertThat(res, containsString("Logged in as " + u2.getPreferredName() + ", acting as " + org2.getName()));

    }

    @Test
    public void testSwitchOrgToSameOrg() throws IOException, GigiApiException {

        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org:" + org1.getId() + "=y", 0));
        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org:" + org1.getId() + "=y", 0));

        String res = IOUtils.readURL(get(SwitchOrganisation.PATH));
        assertThat(res, containsString("Logged in as " + u2.getPreferredName() + ", acting as " + org1.getName()));

    }

    @Test
    public void testSwitchOrgToNonOrg() throws IOException, GigiApiException {

        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org:" + org1.getId() + "=y", 0));
        String res = IOUtils.readURL(post(SwitchOrganisation.PATH, "org:5000=y"));
        assertThat(res, containsString("Context switch failed"));

    }

    @Test
    public void testSwitchOrgToPersonal() throws IOException, GigiApiException {

        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org:" + org1.getId() + "=y", 0));
        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org-leave=personal", 0));

        String res = IOUtils.readURL(get(SwitchOrganisation.PATH));
        assertThat(res, containsString("Logged in as " + u2.getPreferredName()));

    }

    @Test
    public void testSwitchOrgPasswordLogin() throws IOException, GigiApiException {
        cookie = login(email, TEST_PASSWORD);
        loginCertificate = null;
        URLConnection uc = get(cookie, SwitchOrganisation.PATH);
        assertEquals(403, ((HttpURLConnection) uc).getResponseCode());
    }

    @Test
    public void testSwitchOrgLoginChallenge() throws IOException, GigiApiException, KeyManagementException, NoSuchAlgorithmException, GeneralSecurityException {
        loginCertificate = cagent;
        loginPrivateKey = pkagent;
        cookie = login(pkagent, cagent.cert());
        String email = createUniqueName() + "@testdom.com";
        User u3 = User.getById(createVerificationUser("testworker", "testname", email, TEST_PASSWORD));
        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + org1.getId(), "email=" + URLEncoder.encode(u3.getEmail(), "UTF-8") + "&do_affiliate=y&master=y", 1));
        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + org2.getId(), "email=" + URLEncoder.encode(u3.getEmail(), "UTF-8") + "&do_affiliate=y&master=y", 1));

        cookie = cookieWithCertificateLogin(u3);
        URLConnection uc = get(cookie, SwitchOrganisation.PATH);
        assertEquals(403, ((HttpURLConnection) uc).getResponseCode());

        addChallenge(u3.getId(), CATSType.ORG_ADMIN_DP_CHALLENGE_NAME);
        clearCaches();
        uc = get(cookie, SwitchOrganisation.PATH);
        assertEquals(200, ((HttpURLConnection) uc).getResponseCode());
    }
}
