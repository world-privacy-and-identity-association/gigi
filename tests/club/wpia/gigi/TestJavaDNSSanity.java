package club.wpia.gigi;

import static org.junit.Assert.*;

import org.junit.Test;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import org.xbill.DNS.TextParseException;

public class TestJavaDNSSanity {

    /**
     * Simple testcase from the dnsjava examples.
     */
    @Test
    public void testJavaDNSSanity() throws TextParseException, NameTooLongException {
        Name n = Name.fromString("www.dnsjava.org");
        Name o = Name.fromString("dnsjava.org");

        assertTrue(n.subdomain(o));
        Name rel = n.relativize(o);
        Name n2 = Name.concatenate(rel, o);
        assertEquals(n2, n);
    }

}
