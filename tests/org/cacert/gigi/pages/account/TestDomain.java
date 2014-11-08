package org.cacert.gigi.pages.account;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URLEncoder;

import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.account.domain.DomainOverview;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class TestDomain extends ManagedTest {

    User u = User.getById(createVerifiedUser("testuser", "testname", uniq + "@testdom.com", TEST_PASSWORD));

    String session = login(uniq + "@testdom.com", TEST_PASSWORD);

    public TestDomain() throws IOException {}

    @Test
    public void testAdd() throws IOException {
        assertNull(addDomain(session, uniq + ".de"));
        assertNotNull(addDomain(session, uniq + ".de"));
    }

    public static String addDomain(String session, String domain) throws IOException {
        return executeBasicWebInteraction(session, DomainOverview.PATH, "adddomain&newdomain=" + URLEncoder.encode(domain, "UTF-8"), 1);
    }
}
