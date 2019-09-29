package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Country.CountryCodeType;
import club.wpia.gigi.dbObjects.NamePart.NamePartType;
import club.wpia.gigi.dbObjects.Verification.VerificationType;
import club.wpia.gigi.testUtils.ClientBusinessTest;
import club.wpia.gigi.util.Notary;

public class TestVerifyName extends ClientBusinessTest {

    @Test
    public void testIt() throws GigiApiException {
        User u0 = User.getById(createVerificationUser("f", "l", createUniqueName() + "@email.com", TEST_PASSWORD));
        Name n2 = new Name(u, new NamePart(NamePartType.SINGLE_NAME, "Testiaa"));
        Name n3 = new Name(u, new NamePart(NamePartType.SINGLE_NAME, "Testiaa"));
        Name n4 = new Name(u, new NamePart(NamePartType.SINGLE_NAME, "Testiaac"));

        assertEquals(0, n0.getVerificationPoints());
        Notary.verify(u0, u, n0, u.getDoB(), 10, "test mgr", validVerificationDateString(), VerificationType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        assertEquals(10, n0.getVerificationPoints());
        Notary.verify(u0, u, n2, u.getDoB(), 10, "test mgr", validVerificationDateString(), VerificationType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        assertEquals(10, n2.getVerificationPoints());
        Notary.verify(u0, u, n3, u.getDoB(), 10, "test mgr", validVerificationDateString(), VerificationType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        assertEquals(10, n3.getVerificationPoints());
        Notary.verify(u0, u, n4, u.getDoB(), 10, "test mgr", validVerificationDateString(), VerificationType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        assertEquals(10, n4.getVerificationPoints());
        assertEquals(10, u.getMaxVerifyPoints());
    }

    @Test
    public void testValidVerification() throws GigiApiException {
        User u0 = User.getById(createVerifiedUser("f", "l", createUniqueName() + "@email.com", TEST_PASSWORD));
        assertFalse(u0.getPreferredName().isValidVerification());

        add100Points(u0.getId());
        assertTrue(u0.getPreferredName().isValidVerification());

        setVerificationDateToPast(u0.getPreferredName());
        assertFalse(u0.getPreferredName().isValidVerification());

        add100Points(u0.getId());
        assertTrue(u0.getPreferredName().isValidVerification());
    }

}
