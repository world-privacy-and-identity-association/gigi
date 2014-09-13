package org.cacert.gigi.testUtils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.pages.account.domain.DomainOverview;
import org.junit.Before;

public abstract class PingTest extends ClientTest {

    protected static void updateService(String token, String value, String action) throws IOException, MalformedURLException {
        String manage = getTestProps().getProperty("domain.manage");
        String url = manage + "t1=" + token + "&t2=" + value + "&action=" + action;
        assertEquals(200, ((HttpURLConnection) new URL(url).openConnection()).getResponseCode());
    }

    protected void waitForPings(int count) throws SQLException, InterruptedException {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT COUNT(*) FROM domainPinglog");
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 10000) {
            GigiResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) >= count) {
                break;
            }
            Thread.sleep(200);
        }
    }

    protected URL sendDomainForm(URL u, String content) throws IOException, MalformedURLException {
        URLConnection openConnection = u.openConnection();
        openConnection.setRequestProperty("Cookie", cookie);
        openConnection.setDoOutput(true);
        openConnection.getOutputStream().write(content.getBytes());
        openConnection.getHeaderField("Location");

        String newcontent = IOUtils.readURL(cookie(u.openConnection(), cookie));
        Pattern dlink = Pattern.compile(DomainOverview.PATH + "([0-9]+)'>");
        Matcher m1 = dlink.matcher(newcontent);
        m1.find();
        URL u2 = new URL(u.toString() + m1.group(1));
        return u2;
    }

    protected Matcher initailizeDomainForm(URL u) throws IOException, Error {
        URLConnection openConnection = u.openConnection();
        openConnection.setRequestProperty("Cookie", cookie);
        String content1 = IOUtils.readURL(openConnection);
        csrf = getCSRF(1, content1);

        Pattern p = Pattern.compile("([A-Za-z0-9]+)._cacert._auth IN TXT ([A-Za-z0-9]+)");
        Matcher m = p.matcher(content1);
        m.find();
        return m;
    }

    private static boolean first = true;

    @Before
    public void purgeDbAfterTest() throws SQLException, IOException {
        if (first) {
            first = false;
            return;
        }
        purgeDatabase();
    }

}
