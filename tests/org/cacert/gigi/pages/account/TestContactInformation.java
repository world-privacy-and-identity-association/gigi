package org.cacert.gigi.pages.account;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.junit.Test;

public class TestContactInformation extends ClientTest {

    @Test
    public void testDirectoryListingToggle() throws IOException {
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "listme=1&contactinfo=&processContact", 1));
        URLConnection url = new URL("https://" + getServerName() + MyDetails.PATH).openConnection();
        url.setRequestProperty("Cookie", cookie);
        String res = IOUtils.readURL(url);
        res = res.split(java.util.regex.Pattern.quote("</table>"))[1];
        assertThat(res, containsString("value=\"1\" selected"));
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "listme=0&contactinfo=&processContact", 1));
        url = new URL("https://" + getServerName() + MyDetails.PATH).openConnection();
        url.setRequestProperty("Cookie", cookie);
        res = IOUtils.readURL(url);
        res = res.split(java.util.regex.Pattern.quote("</table>"))[1];
        assertTrue(res.contains("value=\"0\" selected"));
    }

    @Test
    public void testContactinfoSet() throws IOException {
        String text = createUniqueName();
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "listme=1&contactinfo=" + text + "&processContact", 1));
        URLConnection url = new URL("https://" + getServerName() + MyDetails.PATH).openConnection();
        url.setRequestProperty("Cookie", cookie);
        String res = IOUtils.readURL(url);
        res = res.split(java.util.regex.Pattern.quote("</table>"))[1];
        assertThat(res, containsString(text));
    }
}
