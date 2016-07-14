package org.cacert.gigi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Locale;

import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.ClientBusinessTest;
import org.junit.Test;

public class TestObjectCache extends ClientBusinessTest {

    @Test
    public void testUserCache() throws SQLException, GigiApiException {
        assertThat(User.getById(id), is(sameInstance(User.getById(id))));

        Calendar c = Calendar.getInstance();
        c.set(1950, 1, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        User u = createUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD);

        assertThat(u, is(sameInstance(User.getById(u.getId()))));
        assertThat(User.getById(u.getId()), is(sameInstance(User.getById(u.getId()))));

    }

    @Test
    public void testDomainCache() throws GigiApiException {
        Domain d = new Domain(u, u, "example.org");

        assertThat(d, is(sameInstance(Domain.getById(d.getId()))));
        assertThat(Domain.getById(d.getId()), is(sameInstance(Domain.getById(d.getId()))));
    }

    @Test
    public void testEmailCache() throws GigiApiException {
        EmailAddress em = new EmailAddress(u, createUniqueName() + "@example.org", Locale.ENGLISH);

        assertThat(em, is(sameInstance(EmailAddress.getById(em.getId()))));
        assertThat(EmailAddress.getById(em.getId()), is(sameInstance(EmailAddress.getById(em.getId()))));
    }
}
