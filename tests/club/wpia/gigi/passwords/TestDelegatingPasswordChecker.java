package club.wpia.gigi.passwords;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.passwords.DelegatingPasswordChecker;
import club.wpia.gigi.passwords.PasswordChecker;

public class TestDelegatingPasswordChecker {

    @Test
    public void testNoCheckers() {
        DelegatingPasswordChecker checker = new DelegatingPasswordChecker(new PasswordChecker[0]);

        assertNull(checker.checkPassword("", new String[0], ""));
    }

    @Test
    public void testOneChecker() {
        DelegatingPasswordChecker checker = new DelegatingPasswordChecker(new PasswordChecker[] {
            new PasswordChecker() {
                @Override
                public GigiApiException checkPassword(String password, String[] nameParts, String email) {
                    return password.isEmpty() ?
                        new GigiApiException("empty password") :
                        null;
                }
            }
        });

        assertNull(checker.checkPassword("a strong password", new String[0], ""));

        GigiApiException exception = checker.checkPassword("", new String[0], "");
        assertNotNull(exception);
        assertEquals("empty password", exception.getMessage());
    }

    @Test
    public void testTwoCheckers() {
        DelegatingPasswordChecker checker = new DelegatingPasswordChecker(new PasswordChecker[] {
            new PasswordChecker() {
                @Override
                public GigiApiException checkPassword(String password, String[] nameParts, String email) {
                    return password.equals(email) ?
                        new GigiApiException("password = email") :
                        null;
                }
            },
            new PasswordChecker() {
                @Override
                public GigiApiException checkPassword(String password, String[] nameParts, String email) {
                    return password.equals("12345") ?
                        new GigiApiException("12345 is a bad password") :
                        null;
                }
            }
        });

        assertNull(checker.checkPassword("a strong password", new String[0], "email"));

        GigiApiException exception1 = checker.checkPassword("email", new String[0], "email");
        assertNotNull(exception1);
        assertEquals("password = email", exception1.getMessage());

        GigiApiException exception2 = checker.checkPassword("12345", new String[0], "email");
        assertNotNull(exception2);
        assertEquals("12345 is a bad password", exception2.getMessage());

        GigiApiException exception3 = checker.checkPassword("12345", new String[0], "12345");
        assertNotNull(exception3);
        assertThat(exception3.getMessage(), containsString("password = email"));
        assertThat(exception3.getMessage(), containsString("12345 is a bad password"));
    }
}
