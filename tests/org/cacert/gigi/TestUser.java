package org.cacert.gigi;

import java.sql.SQLException;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestUser extends ManagedTest {
	@Test
	public void testStoreAndLoad() throws SQLException {
		User u = new User();
		u.setFname("user");
		u.setLname("last");
		u.setMname("");
		u.setSuffix("");
		long dob = System.currentTimeMillis();
		dob -= dob % (1000 * 60 * 60 * 24);
		u.setDob(new java.sql.Date(dob));
		u.setEmail(createUniqueName() + "a@email.org");
		u.insert("password");
		int id = u.getId();
		User u2 = new User(id);
		assertEquals(u, u2);
	}
}
