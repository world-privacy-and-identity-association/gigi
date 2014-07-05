package org.cacert.gigi.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.testUtils.ManagedTest;
import org.cacert.gigi.util.Notary.AssuranceResult;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestNotary extends ManagedTest {
	@Test
	public void testNormalAssurance() throws SQLException {
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
		assertNotEquals(AssuranceResult.ASSURANCE_SUCCEDED, Notary.assure(
				assurer, users[0], -1, "test-notary", "2014-01-01"));
		for (int i = 0; i < result.length; i++) {
			assertEquals(result[i], assurer.getMaxAssurePoints());
			assertNotEquals(AssuranceResult.ASSURANCE_SUCCEDED, Notary.assure(
					assurer, users[i], result[i] + 1, "test-notary",
					"2014-01-01"));
			assertEquals(AssuranceResult.ASSURANCE_SUCCEDED, Notary.assure(
					assurer, users[i], result[i], "test-notary", "2014-01-01"));
			assertNotEquals(AssuranceResult.ASSURANCE_SUCCEDED, Notary.assure(
					assurer, users[i], result[i], "test-notary", "2014-01-01"));
		}

		assertEquals(35, assurer.getMaxAssurePoints());

		assertEquals(2 + 60, assurer.getExperiencePoints());

	}

	@Test
	public void testPoJam() throws SQLException {
		User[] users = new User[30];
		for (int i = 0; i < users.length; i++) {
			int id = createVerifiedUser("fn" + i, "ln" + i, createUniqueName()
					+ "@email.org", "xvXV12°§");
			users[i] = new User(id);
		}
		int id = createAssuranceUser("fn", "ln", createUniqueName()
				+ "@email.org", "xvXV12°§");
		PreparedStatement ps = DatabaseConnection.getInstance().prepare(
				"UPDATE users SET dob=NOW() WHERE id=?");
		ps.setInt(1, id);
		ps.execute();
		User assurer = new User(id);
		for (int i = 0; i < users.length; i++) {
			assertNotEquals(AssuranceResult.ASSURANCE_SUCCEDED, Notary.assure(
					assurer, users[i], -1, "test-notary", "2014-01-01"));
			assertNotEquals(AssuranceResult.ASSURANCE_SUCCEDED, Notary.assure(
					assurer, users[i], 11, "test-notary", "2014-01-01"));
			assertEquals(AssuranceResult.ASSURANCE_SUCCEDED, Notary.assure(
					assurer, users[i], 10, "test-notary", "2014-01-01"));
			assertNotEquals(AssuranceResult.ASSURANCE_SUCCEDED, Notary.assure(
					assurer, users[i], 10, "test-notary", "2014-01-01"));
		}
	}
}
