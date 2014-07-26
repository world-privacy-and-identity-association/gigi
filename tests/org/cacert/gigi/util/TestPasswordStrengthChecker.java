package org.cacert.gigi.util;

import org.cacert.gigi.User;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestPasswordStrengthChecker {

    User u;

    public TestPasswordStrengthChecker() {
        u = new User();
        u.setFname("fname");
        u.setLname("lname");
        u.setMname("mname");
        u.setEmail("email");
        u.setSuffix("suffix");
    }

    @Test
    public void testPasswordLength() {
        assertEquals(1, PasswordStrengthChecker.checkpw("01234", u));
        assertEquals(2, PasswordStrengthChecker.checkpw("0123456789012345", u));
        assertEquals(3, PasswordStrengthChecker.checkpw("012345678901234567890", u));
        assertEquals(4, PasswordStrengthChecker.checkpw("01234567890123456789012345", u));
        assertEquals(5, PasswordStrengthChecker.checkpw("0123456789012345678901234567890", u));
    }

    @Test
    public void testPasswordNonASCII() {
        assertEquals(2, PasswordStrengthChecker.checkpw("0ä", u));
        assertEquals(3, PasswordStrengthChecker.checkpw("0aä", u));
        assertEquals(3, PasswordStrengthChecker.checkpw("0azä", u));
        assertEquals(3, PasswordStrengthChecker.checkpw("0az.ä", u));
    }

    @Test
    public void testPasswordCharTypes() {
        assertEquals(1, PasswordStrengthChecker.checkpw("0", u));
        assertEquals(2, PasswordStrengthChecker.checkpw("0a", u));
        assertEquals(2, PasswordStrengthChecker.checkpw("0az", u));
        assertEquals(3, PasswordStrengthChecker.checkpw("0azZ", u));
        assertEquals(4, PasswordStrengthChecker.checkpw("0a zZ", u));
        assertEquals(5, PasswordStrengthChecker.checkpw("0a. zZ", u));

        assertEquals(1, PasswordStrengthChecker.checkpw(".", u));
        assertEquals(1, PasswordStrengthChecker.checkpw(" ", u));
        assertEquals(1, PasswordStrengthChecker.checkpw("b", u));
        assertEquals(1, PasswordStrengthChecker.checkpw("Z", u));

        assertEquals(2, PasswordStrengthChecker.checkpw("0.", u));
        assertEquals(2, PasswordStrengthChecker.checkpw("0 ", u));
        assertEquals(2, PasswordStrengthChecker.checkpw("0a", u));
        assertEquals(2, PasswordStrengthChecker.checkpw("0Z", u));

        assertEquals(2, PasswordStrengthChecker.checkpw(" .", u));
        assertEquals(2, PasswordStrengthChecker.checkpw(" a", u));
        assertEquals(2, PasswordStrengthChecker.checkpw(" Z", u));

    }

    @Test
    public void testPasswordContains() {
        assertEquals( -1, PasswordStrengthChecker.checkpw("fnamea", u));
        assertEquals( -5, PasswordStrengthChecker.checkpw("na", u));
        assertEquals(0, PasswordStrengthChecker.checkpw("1lname", u));
        assertEquals(0, PasswordStrengthChecker.checkpw("1email", u));
        assertEquals( -1, PasswordStrengthChecker.checkpw("mai", u));
        assertEquals( -1, PasswordStrengthChecker.checkpw("suff", u));
        assertEquals(0, PasswordStrengthChecker.checkpw("1suffix", u));

    }

}
