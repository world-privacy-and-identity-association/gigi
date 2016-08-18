package org.cacert.gigi.pages.account;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URLEncoder;

import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.pages.account.domain.DomainOverview;
import org.cacert.gigi.testUtils.ClientTest;
import org.junit.Test;

public class TestDomain extends ClientTest {

    public TestDomain() throws IOException {}

    @Test
    public void testAdd() throws IOException {
        assertNull(addDomain(cookie, uniq + ".de"));
        assertNotNull(addDomain(cookie, uniq + ".de"));
    }

    @Test
    public void testInvalid() throws IOException {
        assertNotNull(addDomain(cookie, uniq + ".invalid"));
    }

    @Test
    public void testHighFinancialValue() throws IOException {
        assertNotNull(addDomain(cookie, "google.com"));
    }

    @Test
    public void testDelete() throws IOException {
        String domain = uniq + ".de";
        assertNull(addDomain(cookie, domain));
        Domain d0 = Domain.searchUserIdByDomain(domain);
        assertNull(executeBasicWebInteraction(cookie, DomainOverview.PATH, "delete=" + d0.getId(), 0));
        // double delete
        assertNotNull(executeBasicWebInteraction(cookie, DomainOverview.PATH, "delete=" + d0.getId(), 0));
        // re-add
        assertNull(addDomain(cookie, domain));
        Domain d1 = Domain.searchUserIdByDomain(domain);
        assertNotEquals(d0.getId(), d1.getId());
        assertNull(executeBasicWebInteraction(cookie, DomainOverview.PATH, "delete=" + d1.getId(), 0));
    }

    public static String addDomain(String session, String domain) throws IOException {
        return executeBasicWebInteraction(session, DomainOverview.PATH, "adddomain&newdomain=" + URLEncoder.encode(domain, "UTF-8"), 1);
    }
}
