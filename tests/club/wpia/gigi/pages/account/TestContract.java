package club.wpia.gigi.pages.account;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;

public class TestContract extends ClientTest {

    @Test
    public void TestContractSignRevoke() throws IOException {
        // empty contract
        String res = IOUtils.readURL(get(MyContracts.PATH));
        assertThat(res, containsString("This contract concludes an agreement between"));
        assertThat(res, containsString("not yet"));

        // sign contract
        executeBasicWebInteraction(cookie, MyDetails.PATH, "action=signContract", 0);
        getMailReceiver().receive(u.getEmail());
        res = IOUtils.readURL(get(MyContracts.PATH));
        assertThat(res, containsString("This contract concludes an agreement between"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        assertThat(res, containsString(sdf.format(new Date())));

        // sign contract
        executeBasicWebInteraction(cookie, MyDetails.PATH, "action=revokeContract", 0);
        getMailReceiver().receive(u.getEmail());
        res = IOUtils.readURL(get(MyContracts.PATH));
        assertThat(res, containsString("This contract concludes an agreement between"));
        assertThat(res, containsString("not yet"));
    }

    @Test
    public void TestLanguageSwitch() throws IOException {

        String res = IOUtils.readURL(get(MyContracts.PATH));
        assertThat(res, containsString("This contract concludes an agreement between"));
        // switch to German
        executeBasicWebInteraction(cookie, MyDetails.PATH, "lang=de", 0);
        res = IOUtils.readURL(get(MyContracts.PATH));
        assertThat(res, containsString("Dieser Vertrag schließt eine Vereinbarung zwischen"));
        // switch to Turkish, should return default language English
        executeBasicWebInteraction(cookie, MyDetails.PATH, "lang=tr", 0);
        res = IOUtils.readURL(get(MyContracts.PATH));
        assertThat(res, containsString("This contract concludes an agreement between"));
    }

}
