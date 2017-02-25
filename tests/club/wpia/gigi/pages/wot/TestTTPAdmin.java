package club.wpia.gigi.pages.wot;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.admin.TTPAdminPage;
import club.wpia.gigi.testUtils.ClientTest;

public class TestTTPAdmin extends ClientTest {

    User us2;

    public TestTTPAdmin() throws IOException {
        us2 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.com", TEST_PASSWORD));
    }

    @Test
    public void testHasRight() throws IOException, GigiApiException {
        testTTPAdmin(true);
    }

    @Test
    public void testHasNoRight() throws IOException, GigiApiException {
        testTTPAdmin(false);
    }

    public void testTTPAdmin(boolean hasRight) throws IOException, GigiApiException {
        if (hasRight) {
            grant(u, Group.TTP_AGENT);
        }
        grant(u, TTPAdminPage.TTP_APPLICANT);
        cookie = login(u.getEmail(), TEST_PASSWORD);

        assertEquals( !hasRight ? 403 : 200, fetchStatusCode(TTPAdminPage.PATH));
        assertEquals( !hasRight ? 403 : 200, fetchStatusCode(TTPAdminPage.PATH + "/"));
        assertEquals( !hasRight ? 403 : 200, fetchStatusCode(TTPAdminPage.PATH + "/" + u.getId()));
        assertEquals( !hasRight ? 403 : 404, fetchStatusCode(TTPAdminPage.PATH + "/" + us2.getId()));
        assertEquals( !hasRight ? 403 : 404, fetchStatusCode(TTPAdminPage.PATH + "/" + 100));
    }

    private int fetchStatusCode(String path) throws MalformedURLException, IOException {
        return get(path).getResponseCode();
    }
}
