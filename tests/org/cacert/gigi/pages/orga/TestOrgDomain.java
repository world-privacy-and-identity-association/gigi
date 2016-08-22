package org.cacert.gigi.pages.orga;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URLEncoder;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.testUtils.OrgTest;
import org.junit.Test;

public class TestOrgDomain extends OrgTest {

    public TestOrgDomain() throws IOException, GigiApiException {

    }

    @Test
    public void testAdd() throws IOException, GigiApiException {
        Organisation o1 = createUniqueOrg();
        String dom = createUniqueName() + ".de";
        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + o1.getId(), "addDomain&domain=" + URLEncoder.encode(dom, "UTF-8"), 3));
        Domain[] d = o1.getDomains();
        assertEquals(1, d.length);
        assertEquals(dom, d[0].getSuffix());
    }

    @Test
    public void testDel() throws IOException, GigiApiException {
        Organisation o1 = createUniqueOrg();
        String dom = createUniqueName() + ".de";
        Domain d = new Domain(u, o1, dom);
        assertEquals(1, o1.getDomains().length);
        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + o1.getId(), "delete=" + d.getId(), 2));
        assertEquals(0, o1.getDomains().length);
    }

    @Test
    public void testBusinessAddWhileUser() throws IOException, GigiApiException {
        Organisation o1 = createUniqueOrg();
        String dom = createUniqueName() + ".de";
        new Domain(u, u, dom);
        try {
            new Domain(u, o1, dom);
            fail("Was able to add domain twice.");
        } catch (GigiApiException e) {
            assertEquals("Domain could not be inserted. Domain is already known to the system.", e.getMessage());
            // expected
        }
        assertEquals(0, o1.getDomains().length);
        assertEquals(1, u.getDomains().length);
    }

    @Test
    public void testBusinessAddWhileOtherOrg() throws IOException, GigiApiException {
        Organisation o1 = createUniqueOrg();
        Organisation o2 = createUniqueOrg();

        String dom = createUniqueName() + ".de";
        new Domain(u, o1, dom);
        try {
            new Domain(u, o2, dom);
            fail("Was able to add domain twice.");
        } catch (GigiApiException e) {
            assertEquals("Domain could not be inserted. Domain is already known to the system.", e.getMessage());
            // expected
        }
        assertEquals(1, o1.getDomains().length);
        assertEquals(0, o2.getDomains().length);
        assertEquals(0, u.getDomains().length);
    }

    @Test
    public void testBusinessAddInvalid() throws IOException, GigiApiException {
        Organisation o1 = createUniqueOrg();
        String dom = createUniqueName() + ".invalid-tld";
        try {
            new Domain(u, o1, dom);
            fail("Was able to add invalid domain.");
        } catch (GigiApiException e) {
            // expected
        }
        assertEquals(0, o1.getDomains().length);
        assertEquals(0, u.getDomains().length);
    }
}
