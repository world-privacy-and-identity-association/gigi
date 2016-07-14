package org.cacert.gigi.util;

import static org.junit.Assert.*;

import org.cacert.gigi.testUtils.ClientBusinessTest;
import org.junit.Test;

public class TestPasswordStrengthChecker extends ClientBusinessTest {

    String e = "email";

    public TestPasswordStrengthChecker() {}

    private int check(String pw) {
        return PasswordStrengthChecker.checkpw(pw, new String[] {
                "fname", "lname", "mname", "suffix"
        }, e);
    }

    @Test
    public void testPasswordLength() {
        assertEquals(1, check("01234"));
        assertEquals(2, check("0123456789012345"));
        assertEquals(3, check("012345678901234567890"));
        assertEquals(4, check("01234567890123456789012345"));
        assertEquals(5, check("0123456789012345678901234567890"));
    }

    @Test
    public void testPasswordNonASCII() {
        assertEquals(2, check("0ä"));
        assertEquals(3, check("0aä"));
        assertEquals(3, check("0azä"));
        assertEquals(3, check("0az.ä"));
    }

    @Test
    public void testPasswordCharTypes() {
        assertEquals(1, check("0"));
        assertEquals(2, check("0a"));
        assertEquals(2, check("0az"));
        assertEquals(3, check("0azZ"));
        assertEquals(4, check("0a zZ"));
        assertEquals(5, check("0a. zZ"));

        assertEquals(1, check("."));
        assertEquals(1, check(" "));
        assertEquals(1, check("b"));
        assertEquals(1, check("Z"));

        assertEquals(2, check("0."));
        assertEquals(2, check("0 "));
        assertEquals(2, check("0a"));
        assertEquals(2, check("0Z"));

        assertEquals(2, check(" ."));
        assertEquals(2, check(" a"));
        assertEquals(2, check(" Z"));

    }

    @Test
    public void testPasswordContains() {
        assertEquals( -1, check("fnamea"));
        assertEquals( -5, check("na"));
        assertEquals(0, check("1lname"));
        assertEquals(0, check("1email"));
        assertEquals( -1, check("mai"));
        assertEquals( -1, check("suff"));
        assertEquals(0, check("1suffix"));

    }

}
