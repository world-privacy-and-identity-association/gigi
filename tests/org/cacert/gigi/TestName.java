package org.cacert.gigi;

import static org.junit.Assert.*;

import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.NamePart;
import org.cacert.gigi.dbObjects.NamePart.NamePartType;
import org.cacert.gigi.testUtils.ClientBusinessTest;
import org.junit.Before;
import org.junit.Test;

public class TestName extends ClientBusinessTest {

    @Before
    public void setUp() throws Exception {}

    @Test
    public void testNamePartHash() {
        assertEquals(new NamePart(NamePartType.FIRST_NAME, "f"), new NamePart(NamePartType.FIRST_NAME, "f"));
        assertNotEquals(new NamePart(NamePartType.FIRST_NAME, "f"), new NamePart(NamePartType.FIRST_NAME, "f2"));
        assertNotEquals(new NamePart(NamePartType.FIRST_NAME, "f"), new NamePart(NamePartType.LAST_NAME, "f"));
    }

    /**
     * Tests fitness for {@link NamePart#equals(Object)}.
     */
    @Test
    public void testNamePartEquals() {
        NamePart name = new NamePart(NamePartType.FIRST_NAME, "fn");
        assertFalse(name.equals(null));
        assertFalse(name.equals("blargh"));

        // namePart that differs in content
        NamePart diffContent = new NamePart(NamePartType.FIRST_NAME, "f");
        assertFalse(name.equals(diffContent));
        assertFalse(diffContent.equals(name));
        assertTrue(diffContent.equals(diffContent));
        assertTrue(name.equals(name));

        // name part that is equal
        NamePart same = new NamePart(NamePartType.FIRST_NAME, "fn");
        assertTrue(same.equals(name));
        assertTrue(name.equals(same));
        assertTrue(same.equals(same));

        // name part that differs in type
        NamePart diffType = new NamePart(NamePartType.LAST_NAME, "fn");
        assertFalse(diffType.equals(name));
        assertFalse(name.equals(diffType));
        assertTrue(diffType.equals(diffType));

        assertFalse(name.equals("BLA"));
    }

    @Test
    public void testNamePartConstructorCheck() {
        try {
            new NamePart(null, "a");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {

        }
        try {
            new NamePart(NamePartType.FIRST_NAME, null);
            fail("Exception expected");
        } catch (IllegalArgumentException e) {

        }
        try {
            new NamePart(NamePartType.FIRST_NAME, "");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {

        }
    }

    /**
     * Testing {@link Name#matches(String)}. For multiple first names.
     */
    @Test
    public void testMatches() throws GigiApiException {
        Name n0 = new Name(u, new NamePart(NamePartType.FIRST_NAME, "Fir"), new NamePart(NamePartType.FIRST_NAME, "Fir2"), new NamePart(NamePartType.LAST_NAME, "Last"));

        // Having the name "Fir Fir2 Last".
        // This name requires the Last name to be present and at least one of
        // the first names.

        // Simple tests...
        assertTrue(n0.matches("Fir Last"));
        assertFalse(n0.matches("Fir  Last"));
        assertFalse(n0.matches("Fir Last "));
        assertFalse(n0.matches(" Fir Last"));

        // full name
        assertTrue(n0.matches("Fir Fir2 Last"));
        // removing and changing parts
        assertTrue(n0.matches("Fir2 Last"));
        assertFalse(n0.matches("Fir Bast"));
        assertFalse(n0.matches("Fir2 Bast"));
        assertFalse(n0.matches("Fir Fir2 Bast"));
        // only last-name fails
        assertFalse(n0.matches("Last"));
        // one-character first-name is not enough
        assertFalse(n0.matches("F. Last"));
        assertFalse(n0.matches("E. Last"));
        assertFalse(n0.matches("E Last"));
        assertFalse(n0.matches("F Last"));

        assertFalse(n0.matches("Bast"));

        // test the abbreviated name (for e.g in find-RA-Agent-system)
        assertEquals("Fir L.", n0.toAbbreviatedString());
    }

    /**
     * Testing {@link Name#matches(String)} for multiple last-names and a
     * suffix.
     */
    @Test
    public void testMatchesLNSuf() throws GigiApiException {
        Name n0 = new Name(u, new NamePart(NamePartType.FIRST_NAME, "Fir"), new NamePart(NamePartType.LAST_NAME, "Last"), new NamePart(NamePartType.LAST_NAME, "Last2"), new NamePart(NamePartType.SUFFIX, "Suff"));

        // leaving stuff out in order
        assertTrue(n0.matches("Fir Last"));
        assertTrue(n0.matches("Fir Last Last2"));
        assertTrue(n0.matches("Fir Last Last2 Suff"));
        assertTrue(n0.matches("Fir Last Suff"));

        // omitting primary last name
        assertFalse(n0.matches("Fir"));
        assertFalse(n0.matches("Fir Last2"));
        assertFalse(n0.matches("Fir Last2 Suff"));
        assertFalse(n0.matches("Fir Suff"));

        // bringing things out of order
        assertFalse(n0.matches("Fir Last Suff Last2"));
        assertFalse(n0.matches("Fir Suff Last Last2"));
        assertFalse(n0.matches("Fir Suff Last"));
        assertFalse(n0.matches("Fir Last2 Last"));
        assertFalse(n0.matches("Fir Last2 Last Suff"));
    }

    /**
     * Testing {@link Name#matches(String)} for multiple last-names and a
     * suffix.
     */
    @Test
    public void testMatchesDoubleNameParts() throws GigiApiException {
        Name name = new Name(u, //
                new NamePart(NamePartType.FIRST_NAME, "A"), new NamePart(NamePartType.FIRST_NAME, "Fir"), new NamePart(NamePartType.FIRST_NAME, "A"),//
                new NamePart(NamePartType.LAST_NAME, "A"), new NamePart(NamePartType.LAST_NAME, "Last"), new NamePart(NamePartType.LAST_NAME, "A"));

        assertTrue(name.matches("A A"));
        assertTrue(name.matches("Fir A"));
        assertTrue(name.matches("A A Last"));
        assertTrue(name.matches("A A A"));
        assertTrue(name.matches("Fir A A A"));
        assertTrue(name.matches("Fir A A A A"));

        assertFalse(name.matches("A Last"));
        assertFalse(name.matches("Last A"));
        assertFalse(name.matches("Last A Last"));
        assertFalse(name.matches("Fir Last"));
        assertFalse(name.matches("Fir A A A A A"));

    }
}
