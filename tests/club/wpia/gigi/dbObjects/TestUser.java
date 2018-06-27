package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.NamePart.NamePartType;
import club.wpia.gigi.testUtils.ClientBusinessTest;

public class TestUser extends ClientBusinessTest {

    @Test
    public void testGetInitials() throws GigiApiException {
        User u0 = User.getById(createVerificationUser("Kurti", "Hansel", createUniqueName() + "@email.com", TEST_PASSWORD));

        assertEquals("KH", u0.getInitials());

        // single name as preferred name
        Name sName = new Name(u0, new NamePart(NamePartType.SINGLE_NAME, "SingleName"));
        u0.setPreferredName(sName);
        assertEquals("S", u0.getInitials());

        // second western style name as preferred name
        NamePart[] np = {
                new NamePart(NamePartType.FIRST_NAME, "John"), new NamePart(NamePartType.FIRST_NAME, "Walker"), new NamePart(NamePartType.LAST_NAME, "Hansel")
        };
        sName = new Name(u0, np);
        u0.setPreferredName(sName);
        assertEquals("JWH", u0.getInitials());
        // second western style name as preferred name

        NamePart[] np1 = {
                new NamePart(NamePartType.FIRST_NAME, "Dieter"), new NamePart(NamePartType.LAST_NAME, "Hansel"), new NamePart(NamePartType.LAST_NAME, "von"), new NamePart(NamePartType.LAST_NAME, "Hof"), new NamePart(NamePartType.SUFFIX, "Meister")
        };
        sName = new Name(u0, np1);
        u0.setPreferredName(sName);
        assertEquals("DHVHM", u0.getInitials());

        // western style name with dash as preferred name (Hans-Peter)
        NamePart[] np2 = {
                new NamePart(NamePartType.FIRST_NAME, "Hans-Peter"), new NamePart(NamePartType.LAST_NAME, "Hansel")
        };
        sName = new Name(u0, np2);
        u0.setPreferredName(sName);
        assertEquals("HH", u0.getInitials());

        // western style name with dash as separate entry as preferred name
        // (Hans - Peter)
        NamePart[] np3 = {
                new NamePart(NamePartType.FIRST_NAME, "Hans"), new NamePart(NamePartType.FIRST_NAME, "-"), new NamePart(NamePartType.FIRST_NAME, "Joachim"), new NamePart(NamePartType.LAST_NAME, "Hansel")
        };
        sName = new Name(u0, np3);
        u0.setPreferredName(sName);
        assertEquals("HJH", u0.getInitials());

        // western style name with / as separate entry as preferred name
        // (Hans / Peter)
        NamePart[] np4 = {
                new NamePart(NamePartType.FIRST_NAME, "Hans"), new NamePart(NamePartType.FIRST_NAME, "/"), new NamePart(NamePartType.FIRST_NAME, "Peter"), new NamePart(NamePartType.LAST_NAME, "Hansel")
        };
        sName = new Name(u0, np4);
        u0.setPreferredName(sName);
        assertEquals("HPH", u0.getInitials());
    }

}
