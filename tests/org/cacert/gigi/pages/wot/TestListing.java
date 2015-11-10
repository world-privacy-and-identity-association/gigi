package org.cacert.gigi.pages.wot;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.junit.Test;

public class TestListing extends ClientTest {

    @Test
    public void testListing() throws IOException, GigiApiException {
        String c = IOUtils.readURL(get(MyListingPage.PATH));
        // Default settings not listed, empty text
        assertThat(c, not(containsString("value=\"1\" selected")));
        assertThat(c, containsString("value=\"0\" selected"));
        assertThat(c, containsString("></textarea>"));

        assertEquals(302, post(MyListingPage.PATH, "listme=0&contactinfo=a").getResponseCode());
        c = IOUtils.readURL(get(MyListingPage.PATH));
        assertThat(c, not(containsString("value=\"1\" selected")));
        assertThat(c, containsString("value=\"0\" selected"));
        assertThat(c, containsString("></textarea>"));

        assertEquals(302, post(MyListingPage.PATH, "listme=1&contactinfo=a").getResponseCode());
        c = IOUtils.readURL(get(MyListingPage.PATH));
        assertThat(c, containsString("value=\"1\" selected"));
        assertThat(c, not(containsString("value=\"0\" selected")));
        assertThat(c, containsString(">a</textarea>"));

        assertEquals(302, post(MyListingPage.PATH, "listme=1&contactinfo=b").getResponseCode());
        c = IOUtils.readURL(get(MyListingPage.PATH));
        assertThat(c, containsString("value=\"1\" selected"));
        assertThat(c, not(containsString("value=\"0\" selected")));
        assertThat(c, containsString(">b</textarea>"));

        assertEquals(302, post(MyListingPage.PATH, "listme=0&contactinfo=b").getResponseCode());
        c = IOUtils.readURL(get(MyListingPage.PATH));
        assertThat(c, containsString("value=\"0\" selected"));
        assertThat(c, not(containsString("value=\"1\" selected")));
        assertThat(c, containsString("></textarea>"));
    }
}
