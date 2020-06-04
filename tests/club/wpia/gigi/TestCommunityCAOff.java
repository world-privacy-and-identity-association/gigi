package club.wpia.gigi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.Test;

import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;

public class TestCommunityCAOff extends ClientTest {

    protected static boolean isCommunityCATest = false;

    @Test
    public void testMenuLoggedIn() throws GeneralSecurityException, IOException, GigiApiException, InterruptedException {
        String content = IOUtils.readURL(get("/"));

        // Menu SomCA
        // add RA Agent Status in later software version

        // Menu Verification
        testContent(content, "href=\"/wot/ttp\">Request TTP", !isCommunityCATest);
        testContent(content, "href=\"/wot/rules\">Verification Rules", !isCommunityCATest);

        // Menu My Details
        testContent(content, "href=\"/account/find-agent\">Access to Find Agent", !isCommunityCATest);

        assertThat(content, (containsString("Logged in")));

    }

    @Test
    public void testMenuLoggedOut() throws GeneralSecurityException, IOException, GigiApiException, InterruptedException {
        String content = IOUtils.readURL(get("/logout"));
        content = IOUtils.readURL(get("/"));

        // Menu SomCA
        // add RA Agent Status in later software version

        assertThat(content, not((containsString("Logged in"))));

        // text on not login page
        testContent(content, "therefore 6 months only.", isCommunityCATest);
    }

    @Test
    public void testMyDetails() throws GeneralSecurityException, IOException, GigiApiException, InterruptedException {
        String content = IOUtils.readURL(get("/account/details"));
        testContent(content, "RA Agent Contract", !isCommunityCATest);
    }

    private void testContent(String content, String reference, boolean visible) {
        if (visible) {
            assertThat(content, containsString(reference));
        } else {
            assertThat(content, not(containsString(reference)));
        }

    }
}
