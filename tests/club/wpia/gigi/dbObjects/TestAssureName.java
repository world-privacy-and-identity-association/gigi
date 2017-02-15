package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.NamePart;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.dbObjects.Assurance.AssuranceType;
import club.wpia.gigi.dbObjects.Country.CountryCodeType;
import club.wpia.gigi.dbObjects.NamePart.NamePartType;
import club.wpia.gigi.testUtils.ClientBusinessTest;
import club.wpia.gigi.util.Notary;

public class TestAssureName extends ClientBusinessTest {

    @Test
    public void testIt() throws GigiApiException {
        User u0 = User.getById(createAssuranceUser("f", "l", createUniqueName() + "@email.com", TEST_PASSWORD));
        Name n2 = new Name(u, new NamePart(NamePartType.SINGLE_NAME, "Testiaa"));
        Name n3 = new Name(u, new NamePart(NamePartType.SINGLE_NAME, "Testiaa"));
        Name n4 = new Name(u, new NamePart(NamePartType.SINGLE_NAME, "Testiaac"));

        assertEquals(0, n0.getAssurancePoints());
        Notary.assure(u0, u, n0, u.getDoB(), 10, "test mgr", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        assertEquals(10, n0.getAssurancePoints());
        Notary.assure(u0, u, n2, u.getDoB(), 10, "test mgr", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        assertEquals(10, n2.getAssurancePoints());
        Notary.assure(u0, u, n3, u.getDoB(), 10, "test mgr", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        assertEquals(10, n3.getAssurancePoints());
        Notary.assure(u0, u, n4, u.getDoB(), 10, "test mgr", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        assertEquals(10, n4.getAssurancePoints());
        assertEquals(10, u.getMaxAssurePoints());
    }
}
