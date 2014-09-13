package org.cacert.gigi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.ObjectCache;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class TestUserGroupMembership extends ManagedTest {

    private final Group ttpGroup = Group.getByString("ttp-assuer");

    private final Group supporter = Group.getByString("supporter");

    @Test
    public void testAddObject() throws GigiApiException, SQLException {
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));

        User granter = User.getById(createVerifiedUser("grFname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));
        assertBehavesEmpty(u);

        u.grantGroup(granter, ttpGroup);
        assertBehavesTtpGroup(u);

        ObjectCache.clearAllCaches();
        User u2 = User.getById(u.getId());

        assertThat(u2, is(not(sameInstance(u))));
        assertBehavesTtpGroup(u2);

        GigiResultSet rs = fetchGroupRowsFor(u);

        assertTrue(rs.next());
        assertEquals(0, rs.getInt("revokedby"));
        assertEquals(granter.getId(), rs.getInt("grantedby"));
        assertEquals(ttpGroup.getDatabaseName(), rs.getString("permission"));

        assertNull(rs.getDate("deleted"));
        assertNotNull(rs.getDate("granted"));

        assertFalse(rs.next());
    }

    @Test
    public void testRemoveObject() throws GigiApiException, SQLException {
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));

        User granter = User.getById(createVerifiedUser("grFname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));

        assertBehavesEmpty(u);
        u.grantGroup(granter, ttpGroup);
        assertBehavesTtpGroup(u);
        u.revokeGroup(granter, ttpGroup);
        assertBehavesEmpty(u);

        ObjectCache.clearAllCaches();
        User u2 = User.getById(u.getId());
        assertThat(u2, is(not(sameInstance(u))));
        assertBehavesEmpty(u);

        GigiResultSet rs = fetchGroupRowsFor(u);
        assertTrue(rs.next());
        assertEquals(granter.getId(), rs.getInt("revokedby"));
        assertEquals(granter.getId(), rs.getInt("grantedby"));
        assertEquals(ttpGroup.getDatabaseName(), rs.getString("permission"));

        assertNotNull(rs.getDate("deleted"));
        assertNotNull(rs.getDate("granted"));

        assertFalse(rs.next());
    }

    private GigiResultSet fetchGroupRowsFor(User u) throws SQLException {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT * FROM user_groups WHERE user=?");
        ps.setInt(1, u.getId());
        GigiResultSet rs = ps.executeQuery();
        return rs;
    }

    private void assertBehavesEmpty(User u) {
        assertEquals(Collections.emptySet(), u.getGroups());
        assertFalse(u.isInGroup(ttpGroup));
        assertFalse(u.isInGroup(supporter));
    }

    private void assertBehavesTtpGroup(User u) {
        assertEquals(new HashSet<>(Arrays.asList(ttpGroup)), u.getGroups());
        assertTrue(u.isInGroup(ttpGroup));
        assertFalse(u.isInGroup(supporter));
    }
}
