package club.wpia.gigi.pages.orga;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.OrgTest;

public class TestOrgSwitch extends OrgTest {

    private User u2;

    private Organisation org1 = createUniqueOrg();

    private Organisation org2 = createUniqueOrg();

    public TestOrgSwitch() throws IOException, GigiApiException {

        assertEquals(403, get(SwitchOrganisation.PATH).getResponseCode());

        String email = createUniqueName() + "@testdom.com";
        u2 = User.getById(createVerificationUser("testworker", "testname", email, TEST_PASSWORD));
        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + org1.getId(), "email=" + URLEncoder.encode(u2.getEmail(), "UTF-8") + "&do_affiliate=y&master=y", 1));
        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + org2.getId(), "email=" + URLEncoder.encode(u2.getEmail(), "UTF-8") + "&do_affiliate=y&master=y", 1));

        // login with new user u2
        cookie = login(email, TEST_PASSWORD);
    }

    @After
    public void purgeDbAfterTest() throws SQLException, IOException {
        purgeDatabase();
    }

    @Test
    public void testSwitchToOrg() throws IOException, GigiApiException {

        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org:" + org1.getId() + "=y", 0));

        String res = IOUtils.readURL(get(SwitchOrganisation.PATH));
        assertThat(res, containsString("Logged in as " + org1.getName() + " (on behalf of " + u2.getPreferredName()));

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
        assertThat(res, containsString("Logged in as " + org2.getName() + " (on behalf of " + u2.getPreferredName()));

    }

    @Test
    public void testSwitchOrgToSameOrg() throws IOException, GigiApiException {

        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org:" + org1.getId() + "=y", 0));
        assertNull(executeBasicWebInteraction(cookie, SwitchOrganisation.PATH, "org:" + org1.getId() + "=y", 0));

        String res = IOUtils.readURL(get(SwitchOrganisation.PATH));
        assertThat(res, containsString("Logged in as " + org1.getName() + " (on behalf of " + u2.getPreferredName()));

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

}
