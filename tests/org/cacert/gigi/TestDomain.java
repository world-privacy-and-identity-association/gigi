package org.cacert.gigi;

import static org.junit.Assert.*;

import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class TestDomain extends ManagedTest {

    private User us;

    public TestDomain() {
        int uid = createVerifiedUser("fn", "ln", createUniqueName() + "pr@test-email.de", TEST_PASSWORD);
        us = User.getById(uid);
    }

    @Test
    public void testDomain() throws InterruptedException, GigiApiException {
        assertEquals(0, us.getDomains().length);
        Domain d = new Domain(us, "v1example.org");
        Domain[] domains = us.getDomains();
        assertEquals(1, domains.length);
        assertEquals("v1example.org", domains[0].getSuffix());
        assertEquals(domains[0].getOwner().getId(), us.getId());
        assertNotEquals(0, domains[0].getId());
        assertNotEquals(0, d.getId());
        assertEquals(d.getId(), domains[0].getId());

        new Domain(us, "v2-example.org");

        domains = us.getDomains();
        assertEquals(2, domains.length);
        if ( !domains[1].getSuffix().equals("v2-example.org")) {
            Domain d1 = domains[0];
            domains[0] = domains[1];
            domains[1] = d1;
        }
        assertEquals("v2-example.org", domains[1].getSuffix());
        assertEquals(domains[0].getOwner().getId(), us.getId());
        assertEquals(domains[1].getOwner().getId(), us.getId());
        assertNotEquals(0, domains[0].getId());
        assertNotEquals(0, d.getId());
        assertEquals(d.getId(), domains[0].getId());

    }

    @Test
    public void testDoubleDomain() throws InterruptedException, GigiApiException {
        new Domain(us, "dub-example.org");
        try {
            new Domain(us, "dub-example.org");
            fail("expected exception");
        } catch (GigiApiException e) {
            // expected
        }
    }

    @Test
    public void testDoubleDomainDelete() throws InterruptedException, GigiApiException {
        Domain d = new Domain(us, "delexample.org");
        d.delete();
        new Domain(us, "delexample.org");
    }

}
