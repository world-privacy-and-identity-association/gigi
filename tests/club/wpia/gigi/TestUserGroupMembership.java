package club.wpia.gigi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.ObjectCache;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.BusinessTest;

public class TestUserGroupMembership extends BusinessTest {

    private final Group ttpGroup = Group.TTP_ASSURER;

    private final Group supporter = Group.SUPPORTER;

    @Test
    public void testAddObject() throws GigiApiException, SQLException, IOException {
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));

        User granter = getSupporter();
        assertBehavesEmpty(u);

        u.grantGroup(granter, ttpGroup);
        assertBehavesTtpGroup(u);

        ObjectCache.clearAllCaches();
        User u2 = User.getById(u.getId());

        assertThat(u2, is(not(sameInstance(u))));
        assertBehavesTtpGroup(u2);

        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT * FROM `user_groups` WHERE `user`=?")) {
            ps.setInt(1, u.getId());
            GigiResultSet rs = ps.executeQuery();

            assertTrue(rs.next());
            assertEquals(0, rs.getInt("revokedby"));
            assertEquals(granter.getId(), rs.getInt("grantedby"));
            assertEquals(ttpGroup.getDBName(), rs.getString("permission"));

            assertNull(rs.getTimestamp("deleted"));
            assertNotNull(rs.getTimestamp("granted"));

            assertFalse(rs.next());
        }
    }

    @Test
    public void testRemoveObject() throws GigiApiException, SQLException, IOException {
        User u = User.getById(createVerifiedUser("fname", "lname", createUniqueName() + "@example.org", TEST_PASSWORD));

        User granter = getSupporter();

        assertBehavesEmpty(u);
        u.grantGroup(granter, ttpGroup);
        assertBehavesTtpGroup(u);
        u.revokeGroup(granter, ttpGroup);
        assertBehavesEmpty(u);

        ObjectCache.clearAllCaches();
        User u2 = User.getById(u.getId());
        assertThat(u2, is(not(sameInstance(u))));
        assertBehavesEmpty(u);

        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT * FROM `user_groups` WHERE `user`=?")) {
            ps.setInt(1, u.getId());
            GigiResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(granter.getId(), rs.getInt("revokedby"));
            assertEquals(granter.getId(), rs.getInt("grantedby"));
            assertEquals(ttpGroup.getDBName(), rs.getString("permission"));

            assertNotNull(rs.getTimestamp("deleted"));
            assertNotNull(rs.getTimestamp("granted"));

            assertFalse(rs.next());
        }
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

    @Test
    public void testListGroup() throws GigiApiException, IOException {
        Group g = Group.SUPPORTER;
        int start = g.getMembers(0, 10).length;
        User ux = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        User ux2 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        assertEquals(0, g.getMembers(0, 10).length + start);
        ux.grantGroup(getSupporter(), g); // creates a supporter
        assertEquals(2, g.getMembers(0, 10).length + start);
        ux2.grantGroup(ux, g);
        assertEquals(3, g.getMembers(0, 10).length + start);
        ux2.revokeGroup(ux, g);
        assertEquals(2, g.getMembers(0, 10).length + start);
        ux.revokeGroup(ux, g);
        assertEquals(1, g.getMembers(0, 10).length + start);

    }

    @Test
    public void testGroupEquals() {
        assertTrue(ttpGroup.equals(ttpGroup));
        assertFalse(ttpGroup.equals(null));
        assertFalse(ttpGroup.equals(""));
        assertFalse(ttpGroup.equals(supporter));
    }
}
