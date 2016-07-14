package org.cacert.gigi.api;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.Certificate.SANType;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.account.FindAgentAccess;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.RestrictedApiTest;
import org.cacert.gigi.testUtils.TestEmailReceiver.TestMail;
import org.junit.Test;

public class TestFindAgent extends RestrictedApiTest {

    @Test
    public void testResolve() throws GigiApiException, IOException, GeneralSecurityException, InterruptedException {
        Certificate target2 = new Certificate(u, u, Certificate.buildDN("EMAIL", u.getEmail()), Digest.SHA256, generatePEMCSR(generateKeypair(), "EMAIL=" + u.getEmail()), CSRType.CSR, CertificateProfile.getByName("client"), new Certificate.SubjectAlternateName(SANType.EMAIL, "cats@cacert.org"));
        await(target2.issue(null, "2y", u));

        HttpURLConnection v = doApi(FindAgent.PATH_RESOLVE, "serial=" + target2.getSerial().toLowerCase());
        assertEquals(501, v.getResponseCode());
        assertThat(IOUtils.readURL(new InputStreamReader(v.getErrorStream(), "UTF-8")), containsString(FindAgentAccess.PATH));

        grant(u.getEmail(), Group.LOCATE_AGENT);
        v = doApi(FindAgent.PATH_RESOLVE, "serial=" + target2.getSerial().toLowerCase());
        assertEquals(u.getId(), Integer.parseInt(IOUtils.readURL(v)));
    }

    @Test
    public void testMailA() throws GigiApiException, IOException, GeneralSecurityException, InterruptedException {
        testMail(true);
    }

    @Test
    public void testMailB() throws GigiApiException, IOException, GeneralSecurityException, InterruptedException {
        testMail(false);
    }

    public void testMail(boolean userUFirst) throws GigiApiException, IOException, GeneralSecurityException, InterruptedException {
        int u2 = createVerifiedUser("f", "l", createUniqueName() + "@email.com", TEST_PASSWORD);
        User us2 = User.getById(u2);

        // email sending fails
        HttpURLConnection v = doApi(FindAgent.PATH_MAIL, "from=" + id + "&to=" + u2 + "&subject=the-subject&body=body");
        assertEquals(v.getResponseMessage(), 501, v.getResponseCode());
        assertThat(v.getResponseMessage(), containsString("needs to enable access"));

        // even if sender enables service
        grant((userUFirst ? u : us2).getEmail(), Group.LOCATE_AGENT);
        v = doApi(FindAgent.PATH_MAIL, "from=" + id + "&to=" + u2 + "&subject=the-subject&body=body");
        assertEquals(v.getResponseMessage(), 501, v.getResponseCode());
        assertThat(v.getResponseMessage(), containsString("needs to enable access"));

        // receiver needs to enable access as well
        grant((userUFirst ? us2 : u).getEmail(), Group.LOCATE_AGENT);
        v = doApi(FindAgent.PATH_MAIL, "from=" + id + "&to=" + u2 + "&subject=the-subject&body=body");
        assertEquals(v.getResponseMessage(), 200, v.getResponseCode());
        TestMail mail = getMailReceiver().receive();
        assertEquals("body", mail.getMessage());
        assertThat(mail.getSubject(), containsString("the-subject"));
        assertEquals(us2.getEmail(), mail.getTo());
    }

    @Test
    public void testLookupName() throws GigiApiException, IOException, GeneralSecurityException, InterruptedException {
        int u2 = createVerifiedUser("f", "l", createUniqueName() + "@email.com", TEST_PASSWORD);

        String res = IOUtils.readURL(doApi(FindAgent.PATH_INFO, "id=" + id + "&id=" + u2)).replace("\r", "");
        assertEquals(res, "");
        grant(email, Group.LOCATE_AGENT);
        grant(User.getById(u2).getEmail(), Group.LOCATE_AGENT);
        res = IOUtils.readURL(doApi(FindAgent.PATH_INFO, "id=" + id + "&id=" + u2)).replace("\r", "");
        assertEquals(id + ",true," + u.getPreferredName().toAbbreviatedString() + "\n" + u2 + ",false," + User.getById(u2).getPreferredName().toAbbreviatedString() + "\n", res);
    }
}
