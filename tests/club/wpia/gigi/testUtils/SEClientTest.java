package club.wpia.gigi.testUtils;

import static org.junit.Assert.*;

import java.io.IOException;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.pages.admin.support.SupportEnterTicketPage;

/**
 * Superclass for testsuites in a scenario where there is a supporter, who is
 * already logged on.
 */
public abstract class SEClientTest extends ClientTest {

    public SEClientTest() throws IOException, GigiApiException {
        grant(u, Group.SUPPORTER);
        cookie = cookieWithCertificateLogin(u);
        assertEquals(302, post(cookie, SupportEnterTicketPage.PATH, "ticketno=a20140808.8&setTicket=action", 0).getResponseCode());
    }

}
