package org.cacert.gigi.dbObjects;

import static org.junit.Assert.*;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Assurance.AssuranceType;
import org.cacert.gigi.dbObjects.NamePart.NamePartType;
import org.cacert.gigi.testUtils.ClientBusinessTest;
import org.cacert.gigi.util.Notary;
import org.junit.Test;

public class TestAssureName extends ClientBusinessTest {

    @Test
    public void testIt() throws GigiApiException {
        User u0 = User.getById(createAssuranceUser("f", "l", createUniqueName() + "@email.com", TEST_PASSWORD));
        Name n2 = new Name(u, new NamePart(NamePartType.SINGLE_NAME, "Testiaa"));
        Name n3 = new Name(u, new NamePart(NamePartType.SINGLE_NAME, "Testiaa"));
        Name n4 = new Name(u, new NamePart(NamePartType.SINGLE_NAME, "Testiaac"));

        assertEquals(0, n0.getAssurancePoints());
        Notary.assure(u0, u, n0, u.getDoB(), 10, "test mgr", "2010-01-01", AssuranceType.FACE_TO_FACE);
        assertEquals(10, n0.getAssurancePoints());
        Notary.assure(u0, u, n2, u.getDoB(), 10, "test mgr", "2010-01-01", AssuranceType.FACE_TO_FACE);
        assertEquals(10, n2.getAssurancePoints());
        Notary.assure(u0, u, n3, u.getDoB(), 10, "test mgr", "2010-01-01", AssuranceType.FACE_TO_FACE);
        assertEquals(10, n3.getAssurancePoints());
        Notary.assure(u0, u, n4, u.getDoB(), 10, "test mgr", "2010-01-01", AssuranceType.FACE_TO_FACE);
        assertEquals(10, n4.getAssurancePoints());
        assertEquals(10, u.getMaxAssurePoints());
    }
}
