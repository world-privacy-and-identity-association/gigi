package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Date;

import org.junit.Test;

import club.wpia.gigi.dbObjects.CATS;
import club.wpia.gigi.dbObjects.CATS.CATSType;
import club.wpia.gigi.testUtils.ClientBusinessTest;

public class TestCATS extends ClientBusinessTest {

    /**
     * at least 11 months ago (but less than 12), so is inside the window of
     * {@link CATS#TEST_MONTHS}
     */
    private static final Date min11month = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 11 * 31 * 1000L);

    /**
     * at least 12 months ago, so is outside the window of
     * {@link CATS#TEST_MONTHS}
     */
    private static final Date min12month = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 12 * 31 * 1000L);

    public TestCATS() throws GeneralSecurityException, IOException {}

    @Test
    public void testRAChallenge() throws IOException, GeneralSecurityException {
        CATS.enterResult(u, CATSType.AGENT_CHALLENGE, min12month, "en_US", "1");
        assertFalse(CATS.isInCatsLimit(id, CATSType.AGENT_CHALLENGE.getId()));
        CATS.enterResult(u, CATSType.AGENT_CHALLENGE, min11month, "en_US", "1");
        assertTrue(CATS.isInCatsLimit(id, CATSType.AGENT_CHALLENGE.getId()));
    }

    @Test
    public void testCodeSigningChallenge() throws IOException, GeneralSecurityException {
        CATS.enterResult(u, CATSType.CODE_SIGNING_CHALLENGE_NAME, min12month, "en_US", "1");
        assertFalse(CATS.isInCatsLimit(id, CATSType.CODE_SIGNING_CHALLENGE_NAME.getId()));
        CATS.enterResult(u, CATSType.CODE_SIGNING_CHALLENGE_NAME, min11month, "en_US", "1");
        assertTrue(CATS.isInCatsLimit(id, CATSType.CODE_SIGNING_CHALLENGE_NAME.getId()));
    }
}
