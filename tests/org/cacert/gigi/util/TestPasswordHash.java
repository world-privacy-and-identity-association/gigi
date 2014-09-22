package org.cacert.gigi.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestPasswordHash {

    @Test
    public void testVerify() {
        assertTrue(PasswordHash.verifyHash("a", PasswordHash.hash("a")));
        assertTrue(PasswordHash.verifyHash("", PasswordHash.hash("")));
        assertTrue(PasswordHash.verifyHash("a1234", PasswordHash.hash("a1234")));
        assertTrue(PasswordHash.verifyHash("auhlcb4 9x,IUQẞ&lvrvä", PasswordHash.hash("auhlcb4 9x,IUQẞ&lvrvä")));
    }

    @Test
    public void testVerifyNegative() {
        assertFalse(PasswordHash.verifyHash("b", PasswordHash.hash("a")));
        assertFalse(PasswordHash.verifyHash("ae", PasswordHash.hash("auhlcb4 9x,IUQẞ&lvrvä")));
    }
}
