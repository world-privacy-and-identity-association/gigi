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

public class TestPasswordMigration extends ManagedTest {

    @Rule
    public RegisteredUser ru = new RegisteredUser();

    /**
     * Gigi used to support plain SHA-1 password hashes, for compatibility with
     * legacy software. Since there currently is only one accepted hash format,
     * this test now verifies that plain SHA-1 hashes are no longer accepted nor
     * migrated to more recent hash formats.
     *
     * @see PasswordHash.verifyHash
     * @see PasswordHash.hash
     * @throws IOException
     */
    @Test
    public void testNoSHA1PasswordMigration() throws IOException {
        try (GigiPreparedStatement stmt = new GigiPreparedStatement("UPDATE users SET `password`=? WHERE id=?")) {
            stmt.setString(1, "86f7e437faa5a7fce15d1ddcb9eaeaea377667b8"); // sha1("a")
            stmt.setInt(2, ru.getUser().getId());
            stmt.execute();
        }

        String cookie = login(ru.getUser().getEmail(), "a");
        assertFalse(isLoggedin(cookie));

        try (GigiPreparedStatement stmt = new GigiPreparedStatement("SELECT `password` FROM users WHERE id=?")) {
            stmt.setInt(1, ru.getUser().getId());
            GigiResultSet res = stmt.executeQuery();
            assertTrue(res.next());
            String newHash = res.getString(1);
            assertThat(newHash, not(containsString("$")));
        }
    }
}
