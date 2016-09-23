package org.cacert.gigi.api;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.util.Arrays;

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
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

public class TestFindAgent extends RestrictedApiTest {

    @Test
    public void testResolve() throws GigiApiException, IOException, GeneralSecurityException, InterruptedException {
        Certificate target2 = new Certificate(u, u, Certificate.buildDN("EMAIL", u.getEmail()), Digest.SHA256, generatePEMCSR(generateKeypair(), "EMAIL=" + u.getEmail()), CSRType.CSR, CertificateProfile.getByName("client"), new Certificate.SubjectAlternateName(SANType.EMAIL, "cats@cacert.org"));
        await(target2.issue(null, "2y", u));

        HttpURLConnection v = doApi(FindAgent.PATH_RESOLVE, "serial=" + target2.getSerial().toLowerCase());
        assertEquals(501, v.getResponseCode());
        assertThat(IOUtils.readURL(new InputStreamReader(v.getErrorStream(), "UTF-8")), containsString(FindAgentAccess.PATH));

        grant(u, Group.LOCATE_AGENT);
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
        grant((userUFirst ? u : us2), Group.LOCATE_AGENT);
        v = doApi(FindAgent.PATH_MAIL, "from=" + id + "&to=" + u2 + "&subject=the-subject&body=body");
        assertEquals(v.getResponseMessage(), 501, v.getResponseCode());
        assertThat(v.getResponseMessage(), containsString("needs to enable access"));

        // receiver needs to enable access as well
        grant((userUFirst ? us2 : u), Group.LOCATE_AGENT);
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
        res = IOUtils.readURL(doApi(FindAgent.PATH_INFO, "id=" + id + "&id=" + u2)).replace("\r", "");
        assertEquals(new JSONArray().toString(), new JSONArray(new JSONTokener(res)).toString());
        grant(u, Group.LOCATE_AGENT);
        grant(User.getById(u2), Group.LOCATE_AGENT);
        res = IOUtils.readURL(doApi(FindAgent.PATH_INFO, "id=" + id + "&id=" + u2)).replace("\r", "");
        JSONTokener jt = new JSONTokener(res);
        JSONObject j1 = new JSONObject();
        j1.put("id", id);
        j1.put("canAssure", true);
        j1.put("name", u.getPreferredName().toAbbreviatedString());
        JSONObject j2 = new JSONObject();
        j2.put("id", u2);
        j2.put("canAssure", false);
        j2.put("name", User.getById(u2).getPreferredName().toAbbreviatedString());
        JSONArray ja = new JSONArray(Arrays.asList(j1, j2));
        assertEquals(ja.toString(), new JSONArray(jt).toString());
    }
}
