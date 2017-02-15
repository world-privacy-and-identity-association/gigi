package club.wpia.gigi.util;

import static org.junit.Assert.*;

import org.junit.Test;

import club.wpia.gigi.util.PasswordHash;

public class TestPasswordHash {

    @Test
    public void testVerify() {
        assertTrue(PasswordHash.verifyHash("a", PasswordHash.hash("a")) != null);
        assertTrue(PasswordHash.verifyHash("a1234", PasswordHash.hash("a1234")) != null);
        assertTrue(PasswordHash.verifyHash("auhlcb4 9x,IUQẞ&lvrvä", PasswordHash.hash("auhlcb4 9x,IUQẞ&lvrvä")) != null);
    }

    @Test
    public void testVerifyNegative() {
        assertFalse(PasswordHash.verifyHash("b", PasswordHash.hash("a")) != null);
        assertFalse(PasswordHash.verifyHash("ae", PasswordHash.hash("auhlcb4 9x,IUQẞ&lvrvä")) != null);
    }
}
