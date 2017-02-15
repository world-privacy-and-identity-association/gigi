package club.wpia.gigi;

import static org.junit.Assert.*;

import org.junit.Test;

import club.wpia.gigi.database.DatabaseConnection;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.testUtils.ConfiguredTest;

public class TestSQL extends ConfiguredTest {

    @Test
    public void testPrepared() {
        GigiPreparedStatement[] ps = new GigiPreparedStatement[DatabaseConnection.MAX_CACHED_INSTANCES];
        String stmt = "SELECT 1 FROM `users`;";
        for (int i = 0; i < ps.length; i++) {
            assertEquals(i, DatabaseConnection.getInstance().getNumberOfLockedStatements());
            ps[i] = new GigiPreparedStatement(stmt);
        }
        assertEquals(DatabaseConnection.MAX_CACHED_INSTANCES, DatabaseConnection.getInstance().getNumberOfLockedStatements());
        for (int i = ps.length - 1; i >= 0; i--) {
            ps[i].close();
            assertEquals(i, DatabaseConnection.getInstance().getNumberOfLockedStatements());
        }
        for (int i = 0; i < ps.length; i++) {
            assertEquals(i, DatabaseConnection.getInstance().getNumberOfLockedStatements());
            ps[i] = new GigiPreparedStatement(stmt);
        }
    }
}
