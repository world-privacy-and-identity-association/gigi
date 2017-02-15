package club.wpia.gigi.pages.wot;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.ObjectCache;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.wot.RequestTTPPage;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;

public class TestTTP extends ClientTest {

    public TestTTP() throws IOException {}

    @Test
    public void testTTPApply() throws IOException {
        String ttp = IOUtils.readURL(get(RequestTTPPage.PATH));
        assertThat(ttp, containsString("<form"));
        assertNull(executeBasicWebInteraction(cookie, RequestTTPPage.PATH, "country=0"));

        ttp = IOUtils.readURL(get(RequestTTPPage.PATH));
        assertThat(ttp, not(containsString("<form")));
        ObjectCache.clearAllCaches();
        u = User.getById(u.getId());
        assertTrue(u.isInGroup(Group.TTP_APPLICANT));
    }

    @Test
    public void testTTPEnoughPoints() throws IOException, GigiApiException {
        User u = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        cookie = login(u.getEmail(), TEST_PASSWORD);

        String ttp = IOUtils.readURL(get(RequestTTPPage.PATH));
        assertThat(ttp, not(containsString("<form")));
    }
}
