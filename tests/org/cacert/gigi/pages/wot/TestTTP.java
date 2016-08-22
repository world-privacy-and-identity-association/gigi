package org.cacert.gigi.pages.wot;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.ObjectCache;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.junit.Test;

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
        assertTrue(u.isInGroup(Group.getByString("ttp-applicant")));
    }

    @Test
    public void testTTPEnoughPoints() throws IOException, GigiApiException {
        User u = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        cookie = login(u.getEmail(), TEST_PASSWORD);

        String ttp = IOUtils.readURL(get(RequestTTPPage.PATH));
        assertThat(ttp, not(containsString("<form")));
    }
}
