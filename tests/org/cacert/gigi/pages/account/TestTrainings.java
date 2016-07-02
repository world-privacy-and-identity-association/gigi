package org.cacert.gigi.pages.account;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Date;

import org.cacert.gigi.dbObjects.CATS;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.util.HTMLEncoder;
import org.junit.Test;

public class TestTrainings extends ClientTest {

    public TestTrainings() throws GeneralSecurityException, IOException {}

    @Test
    public void testShow() throws IOException, GeneralSecurityException {
        CATS.enterResult(u, CATS.ASSURER_CHALLENGE_NAME, new Date(System.currentTimeMillis()), "en_US", "1");
        CATS.enterResult(u, "Special Case Test", new Date(System.currentTimeMillis()), "spLan", "v23");
        String res = IOUtils.readURL(get(UserTrainings.PATH));
        assertThat(res, containsString("Special Case Test"));
        assertThat(res, containsString(HTMLEncoder.encodeHTML(CATS.ASSURER_CHALLENGE_NAME)));
        assertThat(res, containsString("en_US, 1"));
        assertThat(res, containsString("v23"));
        assertThat(res, containsString("spLan"));
    }
}
