package org.cacert.gigi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;

import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class LoginTest extends ManagedTest {

    @Test
    public void testLoginUnverified() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        registerUser("an", "bn", email, TEST_PASSWORD);
        waitForMail();
        assertFalse(isLoggedin(login(email, TEST_PASSWORD)));
    }

    @Test
    public void testLoginVerified() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        createVerifiedUser("an", "bn", email, TEST_PASSWORD);
        assertTrue(isLoggedin(login(email, TEST_PASSWORD)));
    }

    @Test
    public void testLoginWrongPassword() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        createVerifiedUser("an", "bn", email, TEST_PASSWORD);
        assertFalse(isLoggedin(login(email, TEST_PASSWORD + "b")));
    }

    @Test
    public void testLogoutVerified() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        createVerifiedUser("an", "bn", email, TEST_PASSWORD);
        String cookie = login(email, TEST_PASSWORD);
        assertTrue(isLoggedin(cookie));
        logout(cookie);
        assertFalse(isLoggedin(cookie));
    }

    private void logout(String cookie) throws IOException {
        cookie(new URL("https://" + getServerName() + "/logout").openConnection(), cookie).getHeaderField("Location");
    }

}
