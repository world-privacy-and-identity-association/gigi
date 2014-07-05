package org.cacert.gigi.util;

import java.sql.SQLException;

import org.cacert.gigi.User;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestNotary extends ManagedTest {
	@Test
	public void testSigning() throws SQLException {
		User[] users = new User[30];
		for (int i = 0; i < users.length; i++) {
			int id = createVerifiedUser("fn" + i, "ln" + i, createUniqueName()
					+ "@email.org", "xvXV12°§");
			users[i] = new User(id);
		}
		User assurer = new User(createAssuranceUser("fn", "ln",
				createUniqueName() + "@email.org", "xvXV12°§"));
		int[] result = new int[]{10, 10, 10, 10, 15, 15, 15, 15, 15, 20, 20,
				20, 20, 20, 25, 25, 25, 25, 25, 30, 30, 30, 30, 30, 35, 35, 35,
				35, 35, 35};

		System.out.println(result.length);
		assertFalse(Notary.assure(assurer, users[0], -1, "test-notary",
				"2014-01-01"));
		for (int i = 0; i < result.length; i++) {
			assertEquals(result[i], assurer.getMaxAssurePoints());
			assertFalse(Notary.assure(assurer, users[i], result[i] + 1,
					"test-notary", "2014-01-01"));
			assertTrue(Notary.assure(assurer, users[i], result[i],
					"test-notary", "2014-01-01"));
			assertFalse(Notary.assure(assurer, users[i], result[i],
					"test-notary", "2014-01-01"));
		}

		assertEquals(35, assurer.getMaxAssurePoints());

		assertEquals(2 + 60, assurer.getExperiencePoints());

	}
}
