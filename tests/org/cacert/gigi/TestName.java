package org.cacert.gigi;

import static org.junit.Assert.*;

import org.cacert.gigi.dbObjects.Name;
import org.junit.Before;
import org.junit.Test;

public class TestName {

    Name n = new Name("fn", "ln", "mn", "sf");

    @Before
    public void setUp() throws Exception {}

    @Test
    public void testHashCode() {
        assertEquals(new Name("fname", "lname", null, null).hashCode(), new Name("fname", "lname", null, null).hashCode());
        assertNotEquals(new Name("fname", "lname", null, null).hashCode(), new Name("fname", "lname", null, "b").hashCode());
        assertNotEquals(new Name("fname", "lname", null, null).hashCode(), new Name("fname", "lname", "b", null).hashCode());
        assertNotEquals(new Name("fname", "lname", null, null).hashCode(), new Name("fname", "name", null, null).hashCode());
        assertNotEquals(new Name("fname", "lname", null, null).hashCode(), new Name("name", "lname", null, null).hashCode());
    }

    @Test
    public void testEqualsObject() {
        assertFalse(n.equals(null));
        assertFalse(n.equals("blargh"));
        Name nullname = new Name(null, null, null, null);
        assertFalse(n.equals(nullname));
        assertFalse(nullname.equals(n));
        assertTrue(nullname.equals(nullname));
        assertTrue(n.equals(n));
    }

    @Test
    public void testMatches() {
        assertTrue(n.matches("fn ln"));
        assertTrue(n.matches("fn ln sf"));
        assertTrue(n.matches("fn mn ln sf"));
        assertFalse(n.matches("blargh"));
    }

}
