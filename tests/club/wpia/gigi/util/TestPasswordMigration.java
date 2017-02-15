package club.wpia.gigi.util;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;

import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.testUtils.ManagedTest;
import club.wpia.gigi.testUtils.RegisteredUser;
import club.wpia.gigi.util.PasswordHash;

public class TestPasswordMigration extends ManagedTest {

    @Rule
    public RegisteredUser ru = new RegisteredUser();

    @Test
    public void testPasswordMigration() throws IOException {
        try (GigiPreparedStatement stmt = new GigiPreparedStatement("UPDATE users SET `password`=? WHERE id=?")) {
            stmt.setString(1, PasswordHash.sha1("a"));
            stmt.setInt(2, ru.getUser().getId());
            stmt.execute();
        }
        String cookie = login(ru.getUser().getEmail(), "a");
        assertTrue(isLoggedin(cookie));

        try (GigiPreparedStatement stmt = new GigiPreparedStatement("SELECT `password` FROM users WHERE id=?")) {
            stmt.setInt(1, ru.getUser().getId());
            GigiResultSet res = stmt.executeQuery();
            assertTrue(res.next());
            String newHash = res.getString(1);
            assertThat(newHash, containsString("$"));
        }
    }
}
