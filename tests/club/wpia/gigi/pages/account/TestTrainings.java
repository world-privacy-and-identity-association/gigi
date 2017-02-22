package club.wpia.gigi.pages.account;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Date;

import org.junit.Test;

import club.wpia.gigi.dbObjects.CATS;
import club.wpia.gigi.dbObjects.CATS.CATSType;
import club.wpia.gigi.pages.account.UserTrainings;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.util.HTMLEncoder;

public class TestTrainings extends ClientTest {

    public TestTrainings() throws GeneralSecurityException, IOException {}

    @Test
    public void testShow() throws IOException, GeneralSecurityException {
        CATS.enterResult(u, CATSType.AGENT_CHALLENGE, new Date(System.currentTimeMillis()), "en_US", "1");
        CATS.enterResult(u, "Special Case Test", new Date(System.currentTimeMillis()), "spLan", "v23");
        String res = IOUtils.readURL(get(UserTrainings.PATH));
        assertThat(res, containsString("Special Case Test"));
        assertThat(res, containsString(HTMLEncoder.encodeHTML(CATSType.AGENT_CHALLENGE.getDisplayName())));
        assertThat(res, containsString("en_US, 1"));
        assertThat(res, containsString("v23"));
        assertThat(res, containsString("spLan"));
    }
}
