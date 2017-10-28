package club.wpia.gigi.pages;

import static org.junit.Assert.*;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.CustomMatcher;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.EmailAddress;
import club.wpia.gigi.testUtils.ClientBusinessTest;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;

public class TestVerify extends ClientBusinessTest {

    @Test
    public void testVerify() throws GigiApiException {
        EmailAddress ea = new EmailAddress(u, "test@example.com", Locale.ENGLISH);
        TestMail tm = getMailReceiver().receive(ea.getAddress());
        Pattern p = Pattern.compile(".*hash=(.*)");
        Matcher m = p.matcher(tm.extractLink());
        assertTrue(m.matches());
        String correctToken = m.group(1);

        // Assert initial state
        assertFalse(ea.isVerified());

        // wrong hashes
        assertFalse(ea.isVerifyable(correctToken + "w"));
        assertFalse(ea.isVerifyable(""));
        // correct token
        assertTrue(ea.isVerifyable(correctToken));
        // state transition not possible with wrong token
        org.hamcrest.Matcher<String> isInvalidToken = verificationDoesNotWork(ea);
        assertThat(correctToken + "a", isInvalidToken);
        assertThat("", isInvalidToken);

        // state transition
        assertFalse(ea.isVerified());
        ea.verify(correctToken);
        assertTrue(ea.isVerified());

        // no token is correct anymore
        assertFalse(ea.isVerifyable(correctToken));
        assertFalse(ea.isVerifyable(""));
        // no verification is possible anymore
        assertThat(correctToken + "a", isInvalidToken);
        assertThat(correctToken, isInvalidToken);
        assertThat("", isInvalidToken);
    }

    private org.hamcrest.Matcher<String> verificationDoesNotWork(final EmailAddress ea) {
        return new CustomMatcher<String>("Invalid token for validation.") {

            @Override
            public boolean matches(Object item) {
                if ( !(item instanceof String)) {
                    return false;
                }
                String s = (String) item;
                try {
                    ea.verify(s);
                    return false;
                } catch (IllegalArgumentException e) {
                    return true;
                } catch (GigiApiException e) {
                    throw new Error(e);
                }
            }
        };
    }
}
