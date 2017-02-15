package club.wpia.gigi.pages.admin;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.pages.admin.support.FindUserByDomainPage;
import club.wpia.gigi.pages.admin.support.FindUserByEmailPage;
import club.wpia.gigi.pages.admin.support.SupportEnterTicketPage;
import club.wpia.gigi.testUtils.ClientTest;

public class TestSEAdminTicketSetting extends ClientTest {

    public TestSEAdminTicketSetting() throws IOException, GigiApiException {
        grant(u, Group.SUPPORTER);
        cookie = login(email, TEST_PASSWORD);
    }

    @Test
    public void testFulltextMailSearch() throws MalformedURLException, UnsupportedEncodingException, IOException {
        assertEquals(403, get(FindUserByEmailPage.PATH).getResponseCode());
        assertEquals(302, post(cookie, SupportEnterTicketPage.PATH, "ticketno=a20140808.8&setTicket=action", 0).getResponseCode());
        assertEquals(200, get(FindUserByEmailPage.PATH).getResponseCode());
        assertEquals(200, get(FindUserByDomainPage.PATH).getResponseCode());
        assertEquals(302, post(cookie, SupportEnterTicketPage.PATH, "ticketno=a20140808.8&deleteTicket=action", 0).getResponseCode());
        assertEquals(403, get(FindUserByEmailPage.PATH).getResponseCode());
    }

}
