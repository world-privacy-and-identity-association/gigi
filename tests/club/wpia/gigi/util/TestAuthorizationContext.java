package club.wpia.gigi.util;

import static org.junit.Assert.*;

import org.junit.Test;

import club.wpia.gigi.testUtils.ClientBusinessTest;

public class TestAuthorizationContext extends ClientBusinessTest {

    @Test
    public void testStronglyAuthenticated() {
        AuthorizationContext ac = new AuthorizationContext(u, u, true);
        assertTrue(ac.isStronglyAuthenticated());
    }

    @Test
    public void testNotStronglyAuthenticated() {
        AuthorizationContext ac = new AuthorizationContext(u, u, false);
        assertFalse(ac.isStronglyAuthenticated());
    }

}
