package org.cacert.gigi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.ManagedTest;

import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;

public class TestLanguage extends ManagedTest {

    @Test
    public void testSignupNoLanguage() {
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));
        assertEquals(Locale.ENGLISH, u.getPreferredLocale());
    }

    @Test
    public void testSignupDE() {
        setAcceptLanguage("de");
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));
        assertEquals(Locale.GERMAN, u.getPreferredLocale());
    }

    @Test
    public void testSignupMulti() {
        setAcceptLanguage("de,en");
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));
        assertEquals(Locale.GERMAN, u.getPreferredLocale());
    }

    @Test
    public void testSignupFallback() {
        setAcceptLanguage("ma,de");
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));
        assertEquals(Locale.GERMAN, u.getPreferredLocale());
    }

    @Test
    public void testSignupProjection() {
        setAcceptLanguage("de-de,en");
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));
        assertEquals(Locale.GERMAN, u.getPreferredLocale());
    }

    @Test
    public void testSelectStandard() throws IOException {
        String content = IOUtils.readURL(new URL("https://" + getServerName() + "/").openConnection());
        assertThat(content, containsString("Translations"));
    }

    @Test
    public void testSelectGerman() throws IOException {
        String content = IOUtils.readURL(new URL("https://" + getServerName() + "/?lang=de").openConnection());
        assertThat(content, containsString(Language.getInstance(Locale.GERMAN).getTranslation("Translations")));
    }

    @Test
    public void testLanguageAfterLogin() throws IOException {
        setAcceptLanguage("de,en");
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));
        String cookie = login(u.getEmail(), TEST_PASSWORD);
        String content = IOUtils.readURL(cookie(new URL("https://" + getServerName() + "/").openConnection(), cookie));
        assertThat(content, containsString(Language.getInstance(Locale.GERMAN).getTranslation("Translations")));
    }

    @Test
    public void testOtherLanguageAfterLogin() throws IOException {
        setAcceptLanguage("fr,de,en");
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));
        String cookie = login(u.getEmail(), TEST_PASSWORD);
        String content = IOUtils.readURL(cookie(new URL("https://" + getServerName() + "/").openConnection(), cookie));
        assertThat(content, containsString(Language.getInstance(Locale.FRENCH).getTranslation("Translations")));
    }
}
