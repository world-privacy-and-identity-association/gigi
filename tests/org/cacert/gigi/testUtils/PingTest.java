package org.cacert.gigi.testUtils;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.pages.account.domain.DomainOverview;
import org.cacert.gigi.util.SystemKeywords;
import org.junit.After;

/**
 * Base class for test suites that check extensively if the domain-ping
 * functionality wroks as expected.
 */
public abstract class PingTest extends ClientTest {

    protected String csrf;

    protected static void updateService(String token, String value, String action) throws IOException, MalformedURLException {
        String manage = getTestProps().getProperty("domain.manage");
        assumeNotNull(manage);
        String url = manage + "t1=" + token + "&t2=" + value + "&action=" + action;
        assertEquals(200, ((HttpURLConnection) new URL(url).openConnection()).getResponseCode());
    }

    protected void waitForPings(int count) throws SQLException, InterruptedException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT COUNT(*) FROM `domainPinglog`")) {
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
    }

    protected String sendDomainForm(String content) throws IOException, MalformedURLException {
        URLConnection openConnection = get(DomainOverview.PATH);
        openConnection.setDoOutput(true);
        openConnection.getOutputStream().write(content.getBytes("UTF-8"));
        openConnection.getHeaderField("Location");
        int code = ((HttpURLConnection) openConnection).getResponseCode();
        if (code != 302) {
            throw new Error("Code was: " + code + "\ncontent was: " + fetchStartErrorMessage(IOUtils.readURL(openConnection)));
        }

        String newcontent = IOUtils.readURL(get(DomainOverview.PATH));
        Pattern dlink = Pattern.compile(DomainOverview.PATH + "/([0-9]+)'>");
        Matcher m1 = dlink.matcher(newcontent);
        if ( !m1.find()) {
            throw new Error(newcontent);
        }
        return DomainOverview.PATH + "/" + m1.group(1);
    }

    protected Matcher initailizeDomainForm() throws IOException, Error {
        String content1 = IOUtils.readURL(get(DomainOverview.PATH));
        csrf = getCSRF(1, content1);

        Pattern p = Pattern.compile("([A-Za-z0-9]+)." + SystemKeywords.DNS_PREFIX + "._auth IN TXT ([A-Za-z0-9]+)");
        Matcher m = p.matcher(content1);
        m.find();
        return m;
    }

    @After
    public void purgeDbAfterTest() throws SQLException, IOException {
        purgeDatabase();
    }

}
