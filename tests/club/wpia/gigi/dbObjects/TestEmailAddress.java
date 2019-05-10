package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.testUtils.OrgTest;

public class TestEmailAddress extends OrgTest {

    public TestEmailAddress() throws IOException, GigiApiException {

    }

    @Test
    public void testAddEmail() {
        String email = createUniqueName() + "@email.com";
        assertNull(addEmailAddress(email));

        // add known email address
        assertEquals("The email address is already known to the system.", addEmailAddress(email));

        // add invalid email address
        email = createUniqueName();
        assertEquals("Invalid email.", addEmailAddress(email));

        // add invalid email address
        email = createUniqueName() + "@email";
        assertEquals("Invalid email.", addEmailAddress(email));

        // add email address with organisation domain
        String dom = createUniqueName() + ".de";
        try {
            Organisation o1 = createUniqueOrg();
            new Domain(u, o1, dom);
        } catch (GigiApiException e) {
            // nothing to do
        }
        email = createUniqueName() + "@" + dom;
        assertEquals("The entered email address belongs to a registered organisation. Please contact the organisation to issue certificates for this email address.", addEmailAddress(email));

    }

    private String addEmailAddress(String email) {
        try {
            EmailAddress addr = new EmailAddress(u, email, Locale.ENGLISH);
            getMailReceiver().receive(addr.getAddress());
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (GigiApiException e) {
            return e.getMessage();
        }
        return null;
    }

}
