package org.cacert.gigi.util;

import java.sql.SQLException;

import org.cacert.gigi.User;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestNotary extends ManagedTest {
	@Test
	public void testSigning() throws SQLException {
		User[] users = new User[10];
		for (int i = 0; i < users.length; i++) {
			int id = createVerifiedUser("fn" + i, "ln" + i, createUniqueName()
					+ "@email.org", "xvXV12°§");
			users[i] = new User(id);
		}
		User assurer = new User(createAssuranceUser("fn", "ln",
				createUniqueName() + "@email.org", "xvXV12°§"));
		assertEquals(10, assurer.getMaxAssurePoints());
		assertTrue(Notary.assure(assurer, users[1], 10, "test-notary",
				"2014-01-01"));
		assertEquals(10, assurer.getMaxAssurePoints());
		assertTrue(Notary.assure(assurer, users[2], 10, "test-notary",
				"2014-01-01"));
		assertEquals(10, assurer.getMaxAssurePoints());
		assertTrue(Notary.assure(assurer, users[3], 10, "test-notary",
				"2014-01-01"));
		assertEquals(10, assurer.getMaxAssurePoints());
		assertTrue(Notary.assure(assurer, users[4], 10, "test-notary",
				"2014-01-01"));
		assertEquals(15, assurer.getMaxAssurePoints());
		assertTrue(Notary.assure(assurer, users[5], 15, "test-notary",
				"2014-01-01"));
		// Assure someone again
		assertTrue(!Notary.assure(assurer, users[5], 15, "test-notary",
				"2014-01-01"));

		// Assure too much
		assertTrue(!Notary.assure(assurer, users[6], 20, "test-notary",
				"2014-01-01"));
		assertTrue(!Notary.assure(assurer, users[6], 16, "test-notary",
				"2014-01-01"));

		assertTrue(Notary.assure(assurer, users[6], 15, "test-notary",
				"2014-01-01"));
		assertEquals(15, assurer.getMaxAssurePoints());

		// Assure self
		assertTrue(!Notary.assure(assurer, assurer, 10, "test-notary",
				"2014-01-01"));

		assertTrue(Notary.assure(assurer, users[7], 15, "test-notary",
				"2014-01-01"));
		assertEquals(15, assurer.getMaxAssurePoints());
		assertTrue(Notary.assure(assurer, users[8], 15, "test-notary",
				"2014-01-01"));
		assertEquals(15, assurer.getMaxAssurePoints());
		assertTrue(Notary.assure(assurer, users[9], 15, "test-notary",
				"2014-01-01"));
		assertEquals(20, assurer.getMaxAssurePoints());

		assertTrue(Notary.assure(assurer, users[0], 15, "test-notary",
				"2014-01-01"));
		assertEquals(20, assurer.getMaxAssurePoints());

		assertEquals(2 + 20, assurer.getExperiencePoints());

	}
}
