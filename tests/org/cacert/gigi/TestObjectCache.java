package org.cacert.gigi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Locale;

import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class TestObjectCache extends ManagedTest {

    int uid = createVerifiedUser("fname", "lname", createUniqueName() + "@example.com", TEST_PASSWORD);

    @Test
    public void testUserCache() throws SQLException {
        assertThat(User.getById(uid), is(sameInstance(User.getById(uid))));

        User u = new User();
        u.setName(new Name("fname", "lname", "mname", "suffix"));
        u.setEmail(createUniqueName() + "@example.org");
        Calendar c = Calendar.getInstance();
        c.set(1950, 1, 1);
        u.setDoB(new Date(c.getTime().getTime()));
        u.setPreferredLocale(Locale.ENGLISH);
        u.insert(TEST_PASSWORD);

        assertThat(u, is(sameInstance(User.getById(u.getId()))));
        assertThat(User.getById(u.getId()), is(sameInstance(User.getById(u.getId()))));

    }

    @Test
    public void testDomainCache() throws GigiApiException {
        Domain d = new Domain(User.getById(uid), "example.org");
        d.insert();

        assertThat(d, is(sameInstance(Domain.getById(d.getId()))));
        assertThat(Domain.getById(d.getId()), is(sameInstance(Domain.getById(d.getId()))));
    }

    @Test
    public void testEmailCache() throws GigiApiException {
        EmailAddress em = new EmailAddress(User.getById(uid), createUniqueName() + "@example.org");
        em.insert(Language.getInstance(Locale.ENGLISH));

        assertThat(em, is(sameInstance(EmailAddress.getById(em.getId()))));
        assertThat(EmailAddress.getById(em.getId()), is(sameInstance(EmailAddress.getById(em.getId()))));
    }
}
