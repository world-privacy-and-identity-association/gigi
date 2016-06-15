package org.cacert.gigi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Locale;

import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Assume;
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
        String content = IOUtils.readURL(get("cook", "/"));
        assertThat(content, containsString("Language"));
    }

    @Test
    public void testSelectGerman() throws IOException {
        String content = IOUtils.readURL(get("", "/?lang=de"));
        assertThat(content, containsString(Language.getInstance(Locale.GERMAN).getTranslation("Language")));
    }

    @Test
    public void testLanguageAfterLogin() throws IOException {
        setAcceptLanguage("de,en");
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));
        String cookie = login(u.getEmail(), TEST_PASSWORD);
        String content = IOUtils.readURL(get(cookie, "/"));
        assertThat(content, containsString(Language.getInstance(Locale.GERMAN).getTranslation("Language")));
    }

    @Test
    public void testOtherLanguageAfterLogin() throws IOException {
        Assume.assumeNotNull(Language.getInstance(Locale.FRENCH));
        setAcceptLanguage("fr,de,en");
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));
        String cookie = login(u.getEmail(), TEST_PASSWORD);
        String content = IOUtils.readURL(get(cookie, "/"));
        assertThat(content, containsString(Language.getInstance(Locale.FRENCH).getTranslation("Language")));
    }
}
