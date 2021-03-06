package club.wpia.gigi;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.util.RandomToken;

public class TestPasswordReset extends ClientTest {

    String pub = RandomToken.generateToken(32);

    String priv = RandomToken.generateToken(32);

    int id = u.generatePasswordResetTicket(u, pub, priv);

    @Test
    public void testInternal() throws IOException, GigiApiException {
        User u2 = User.getResetWithToken(id, pub);
        assertSame(u, u2);
        assertNotNull(login(u.getEmail(), TEST_PASSWORD));
        u2.consumePasswordResetTicket(id, priv, TEST_PASSWORD + "'");
        assertEquals("", login(u.getEmail(), TEST_PASSWORD));
        assertNotNull(login(u.getEmail(), TEST_PASSWORD + "'"));
    }

    @Test
    public void testDoubleUse() throws IOException, GigiApiException {
        User u2 = User.getResetWithToken(id, pub);
        assertSame(u, u2);
        assertNotNull(login(u.getEmail(), TEST_PASSWORD));
        u2.consumePasswordResetTicket(id, priv, TEST_PASSWORD + "'");
        assertEquals("", login(u.getEmail(), TEST_PASSWORD));
        assertNotNull(login(u.getEmail(), TEST_PASSWORD + "'"));
        try {
            u2.consumePasswordResetTicket(id, priv, TEST_PASSWORD + "''");
            fail("Exception expected.");
        } catch (GigiApiException e) {
            // expected
        }
        assertNotNull(login(u.getEmail(), TEST_PASSWORD + "'"));
    }

    @Test
    public void testInternalWrongTk() throws IOException, GigiApiException {
        User u2 = User.getResetWithToken(id, pub + "'");
        assertNull(u2);
    }

    @Test
    public void testInternalWrongId() throws IOException, GigiApiException {
        User u2 = User.getResetWithToken(id + 1, pub);
        assertNull(u2);
    }

    @Test(expected = GigiApiException.class)
    public void testInternalWeak() throws IOException, GigiApiException {
        u.consumePasswordResetTicket(id, priv, "");
    }

    @Test(expected = GigiApiException.class)
    public void testInternalWrongPriv() throws IOException, GigiApiException {
        u.consumePasswordResetTicket(id, priv + "'", TEST_PASSWORD);
    }

    @Test(expected = GigiApiException.class)
    public void testInternalWrongIdSetting() throws IOException, GigiApiException {
        u.consumePasswordResetTicket(id + 1, priv, TEST_PASSWORD);
    }
}
