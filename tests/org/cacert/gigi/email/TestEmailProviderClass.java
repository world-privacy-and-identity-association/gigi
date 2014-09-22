package org.cacert.gigi.email;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.util.Properties;

import org.cacert.gigi.testUtils.ConfiguredTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestEmailProviderClass extends ConfiguredTest {

    @Test
    public void testNonmail() throws IOException {
        String result = EmailProvider.getInstance().checkEmailServer(0, "nomail");
        assertNotEquals(EmailProvider.OK, result);
    }

    @Test
    public void testFastcheckSucceed() throws IOException {
        String succmail = getTestProps().getProperty("email.address");
        assumeNotNull(succmail);

        String result = EmailProvider.getInstance().checkEmailServer(0, succmail);
        assertEquals(EmailProvider.OK, result);
    }

    @Test
    public void testFastcheckFail() throws IOException {
        String failmail = getTestProps().getProperty("email.non-address");
        assumeNotNull(failmail);

        String result = EmailProvider.getInstance().checkEmailServer(0, failmail);
        assertNotEquals(EmailProvider.OK, result);
    }

    @BeforeClass
    public static void initMailsystem() {
        Properties prop = new Properties();
        prop.setProperty("emailProvider", "org.cacert.gigi.email.Sendmail");
        EmailProvider.initSystem(prop, null, null);
    }
}
