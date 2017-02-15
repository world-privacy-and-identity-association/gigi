package club.wpia.gigi.pages.account;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URLEncoder;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.pages.account.ChangePasswordPage;
import club.wpia.gigi.testUtils.ClientTest;

public class TestChangePassword extends ClientTest {

    String path = ChangePasswordPage.PATH;

    public TestChangePassword() throws IOException {
        cookie = login(u.getEmail(), TEST_PASSWORD);
        assertTrue(isLoggedin(cookie));
    }

    @Test
    public void testChangePasswordInternal() throws IOException, GigiApiException {
        try {
            u.changePassword(TEST_PASSWORD + "wrong", TEST_PASSWORD + "v2");
            fail("Password change must not succeed if old password is wrong.");
        } catch (GigiApiException e) {
            // expected
        }
        ;
        assertTrue(isLoggedin(login(u.getEmail(), TEST_PASSWORD)));
        u.changePassword(TEST_PASSWORD, TEST_PASSWORD + "v2");
        assertTrue(isLoggedin(login(u.getEmail(), TEST_PASSWORD + "v2")));
    }

    @Test
    public void testChangePasswordWeb() throws IOException {
        String error = executeBasicWebInteraction(cookie, path,
                "oldpassword=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") //
                        + "&pword1=" + URLEncoder.encode(TEST_PASSWORD + "v2", "UTF-8")//
                        + "&pword2=" + URLEncoder.encode(TEST_PASSWORD + "v2", "UTF-8"));
        assertNull(error);
        assertTrue(isLoggedin(login(u.getEmail(), TEST_PASSWORD + "v2")));
        assertFalse(isLoggedin(login(u.getEmail(), TEST_PASSWORD)));

    }

    @Test
    public void testChangePasswordWebOldWrong() throws IOException {
        String error = executeBasicWebInteraction(cookie, path,
                "oldpassword=a" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") //
                        + "&pword1=" + URLEncoder.encode(TEST_PASSWORD + "v2", "UTF-8")//
                        + "&pword2=" + URLEncoder.encode(TEST_PASSWORD + "v2", "UTF-8"));
        assertNotNull(error);
        assertFalse(isLoggedin(login(u.getEmail(), TEST_PASSWORD + "v2")));
        assertTrue(isLoggedin(login(u.getEmail(), TEST_PASSWORD)));

    }

    @Test
    public void testChangePasswordWebNewWrong() throws IOException {
        String error = executeBasicWebInteraction(cookie, path,
                "oldpassword=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") //
                        + "&pword1=" + URLEncoder.encode(TEST_PASSWORD + "v2", "UTF-8")//
                        + "&pword2=a" + URLEncoder.encode(TEST_PASSWORD + "v2", "UTF-8"));
        assertNotNull(error);
        assertFalse(isLoggedin(login(u.getEmail(), TEST_PASSWORD + "v2")));
        assertTrue(isLoggedin(login(u.getEmail(), TEST_PASSWORD)));

    }

    @Test
    public void testChangePasswordWebNewEasy() throws IOException {
        String error = executeBasicWebInteraction(cookie, path, "oldpassword=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") //
                + "&pword1=a&pword2=a");
        assertNotNull(error);
        assertFalse(isLoggedin(login(u.getEmail(), TEST_PASSWORD + "v2")));
        assertTrue(isLoggedin(login(u.getEmail(), TEST_PASSWORD)));

    }

    @Test
    public void testChangePasswordWebMissingFields() throws IOException {
        String np = URLEncoder.encode(TEST_PASSWORD + "v2", "UTF-8");
        assertTrue(isLoggedin(login(u.getEmail(), TEST_PASSWORD)));
        String error = executeBasicWebInteraction(cookie, path, "oldpassword=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") //
                + "&pword1=" + np);
        assertNotNull(error);
        assertFalse(isLoggedin(login(u.getEmail(), TEST_PASSWORD + "v2")));
        assertTrue(isLoggedin(login(u.getEmail(), TEST_PASSWORD)));

        error = executeBasicWebInteraction(cookie, path, "oldpassword=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") //
                + "&pword2=" + np);
        assertNotNull(error);
        assertFalse(isLoggedin(login(u.getEmail(), TEST_PASSWORD + "v2")));
        assertTrue(isLoggedin(login(u.getEmail(), TEST_PASSWORD)));

        error = executeBasicWebInteraction(cookie, path, "pword1=" + np + "&pword2=" + np);
        assertNotNull(error);
        assertFalse(isLoggedin(login(u.getEmail(), TEST_PASSWORD + "v2")));
        assertTrue(isLoggedin(login(u.getEmail(), TEST_PASSWORD)));

    }

}
