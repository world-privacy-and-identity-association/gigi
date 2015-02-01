package org.cacert.gigi.pages.wot;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.admin.TTPAdminPage;
import org.cacert.gigi.testUtils.ClientTest;
import org.junit.Test;

public class TestTTPAdmin extends ClientTest {

    User us2;

    public TestTTPAdmin() throws IOException {
        us2 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.com", TEST_PASSWORD));
    }

    @Test
    public void testHasRight() throws IOException {
        testTTPAdmin(true);
    }

    @Test
    public void testHasNoRight() throws IOException {
        testTTPAdmin(false);
    }

    public void testTTPAdmin(boolean hasRight) throws IOException {
        if (hasRight) {
            grant(email, Group.getByString("ttp-assurer"));
        }
        grant(u.getEmail(), TTPAdminPage.TTP_APPLICANT);
        cookie = login(u.getEmail(), TEST_PASSWORD);

        assertEquals( !hasRight ? 403 : 200, fetchStatusCode("https://" + getServerName() + TTPAdminPage.PATH));
        assertEquals( !hasRight ? 403 : 200, fetchStatusCode("https://" + getServerName() + TTPAdminPage.PATH + "/"));
        assertEquals( !hasRight ? 403 : 200, fetchStatusCode("https://" + getServerName() + TTPAdminPage.PATH + "/" + u.getId()));
        assertEquals( !hasRight ? 403 : 404, fetchStatusCode("https://" + getServerName() + TTPAdminPage.PATH + "/" + us2.getId()));
        assertEquals( !hasRight ? 403 : 404, fetchStatusCode("https://" + getServerName() + TTPAdminPage.PATH + "/" + 100));
    }

    private int fetchStatusCode(String path) throws MalformedURLException, IOException {
        URL u = new URL(path);
        return ((HttpURLConnection) cookie(u.openConnection(), cookie)).getResponseCode();
    }
}
