package org.cacert.gigi.util;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.testUtils.ManagedTest;
import org.cacert.gigi.testUtils.RegisteredUser;
import org.junit.Rule;
import org.junit.Test;

public class TestPasswordMigration extends ManagedTest {

    @Rule
    public RegisteredUser ru = new RegisteredUser();

    @Test
    public void testPasswordMigration() throws IOException {
        GigiPreparedStatement stmt = DatabaseConnection.getInstance().prepare("UPDATE users SET `password`=SHA1(?) WHERE id=?");
        stmt.setString(1, "a");
        stmt.setInt(2, ru.getUser().getId());
        stmt.execute();
        String cookie = login(ru.getUser().getEmail(), "a");
        assertTrue(isLoggedin(cookie));

        stmt = DatabaseConnection.getInstance().prepare("SELECT `password` FROM users WHERE id=?");
        stmt.setInt(1, ru.getUser().getId());
        GigiResultSet res = stmt.executeQuery();
        assertTrue(res.next());
        String newHash = res.getString(1);
        assertThat(newHash, containsString("$"));
    }
}
