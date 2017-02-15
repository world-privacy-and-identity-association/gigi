package club.wpia.gigi;

import static org.junit.Assert.*;

import org.junit.Test;

import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.testUtils.ClientBusinessTest;

public class TestDomain extends ClientBusinessTest {

    @Test
    public void testDomain() throws InterruptedException, GigiApiException {
        assertEquals(0, u.getDomains().length);
        Domain d = new Domain(u, u, "v1example.org");
        Domain[] domains = u.getDomains();
        assertEquals(1, domains.length);
        assertEquals("v1example.org", domains[0].getSuffix());
        assertEquals(domains[0].getOwner().getId(), u.getId());
        assertNotEquals(0, domains[0].getId());
        assertNotEquals(0, d.getId());
        assertEquals(d.getId(), domains[0].getId());

        new Domain(u, u, "v2-example.org");

        domains = u.getDomains();
        assertEquals(2, domains.length);
        if ( !domains[1].getSuffix().equals("v2-example.org")) {
            Domain d1 = domains[0];
            domains[0] = domains[1];
            domains[1] = d1;
        }
        assertEquals("v2-example.org", domains[1].getSuffix());
        assertEquals(domains[0].getOwner().getId(), u.getId());
        assertEquals(domains[1].getOwner().getId(), u.getId());
        assertNotEquals(0, domains[0].getId());
        assertNotEquals(0, d.getId());
        assertEquals(d.getId(), domains[0].getId());

    }

    @Test
    public void testDoubleDomain() throws InterruptedException, GigiApiException {
        new Domain(u, u, "dub-example.org");
        try {
            new Domain(u, u, "dub-example.org");
            fail("expected exception, was able to insert domain (with different case) a second time");
        } catch (GigiApiException e) {
            // expected
        }
    }

    @Test
    public void testDoubleDomainCase() throws InterruptedException, GigiApiException {
        Domain d = new Domain(u, u, "dub2-ExaMple.Org");
        assertEquals("dub2-example.org", d.getSuffix());
        try {
            new Domain(u, u, "duB2-eXample.oRG");
            fail("expected exception, was able to insert domain (with different case) a second time");
        } catch (GigiApiException e) {
            // expected
        }
    }

    @Test
    public void testDoubleDomainDelete() throws InterruptedException, GigiApiException {
        Domain d = new Domain(u, u, "delexample.org");
        d.delete();
        new Domain(u, u, "delexample.org");
    }

}
