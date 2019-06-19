package club.wpia.gigi;

import static org.junit.Assert.*;

import org.junit.Test;

import club.wpia.gigi.testUtils.ConfiguredTest;
import club.wpia.gigi.util.DomainAssessment;

public class DomainVerification extends ConfiguredTest {

    @Test
    public void testDomainPart() {
        assertTrue(DomainAssessment.isValidDomainPart("wpia"));
        assertTrue(DomainAssessment.isValidDomainPart("de"));
        assertTrue(DomainAssessment.isValidDomainPart("ha2-a"));
        assertTrue(DomainAssessment.isValidDomainPart("ha2--a"));
        assertTrue(DomainAssessment.isValidDomainPart("h--a"));
        assertFalse(DomainAssessment.isValidDomainPart("-xnbla"));
        assertFalse(DomainAssessment.isValidDomainPart("xnbla-"));
        assertFalse(DomainAssessment.isValidDomainPart(""));
        assertTrue(DomainAssessment.isValidDomainPart("2xnbla"));
        assertTrue(DomainAssessment.isValidDomainPart("xnbla2"));
        assertTrue(DomainAssessment.isValidDomainPart("123"));
        assertTrue(DomainAssessment.isValidDomainPart("abcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxy1234567890123"));
        assertFalse(DomainAssessment.isValidDomainPart("abcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxy12345678901234"));
        // test underscore in domain according to BR 7.1.4.2.1
        assertFalse(DomainAssessment.isValidDomainPart("_"));
        assertFalse(DomainAssessment.isValidDomainPart("a_b"));
    }

    @Test
    public void testDomainCertifiable() {
        isCertifiableDomain(true, "wpia.club", false);
        isCertifiableDomain(true, "wpia.de", false);
        isCertifiableDomain(true, "1234.org", false);
        isCertifiableDomain(false, "a.wpia.club", true);
        isCertifiableDomain(false, "gigi.local", true);
        isCertifiableDomain(false, "org", true);
        isCertifiableDomain(false, "'a.org", true);
        isCertifiableDomain(false, ".org", true);
        isCertifiableDomain(false, ".org.", true);
        // non-real-punycode
        isCertifiableDomain(true, "xna-ae.de", false);
        isCertifiableDomain(true, "xn-aae.de", false);

        // illegal punycode:
        // illegal ace prefix
        isCertifiableDomain(false, "aa--b.com", true);
        isCertifiableDomain(false, "xm--ae-a.de", true);

        // illegal punycode content
        isCertifiableDomain(false, "xn--ae-a.com", true);
        isCertifiableDomain(false, "xn--ae.de", true);
        isCertifiableDomain(false, "xn--ae-a.org", true);
        isCertifiableDomain(false, "xn--ae-a.de", true);
        // valid punycode requires permission
        isCertifiableDomain(true, "xn--4ca0bs.de", true);
        isCertifiableDomain(false, "xn--4ca0bs.de", false);
        isCertifiableDomain(true, "xn--a-zfa9cya.de", true);
        isCertifiableDomain(false, "xn--a-zfa9cya.de", false);

        // valid punycode does not help under .com
        isCertifiableDomain(false, "xn--a-zfa9cya.com", true);
        isCertifiableDomain(true, "zfa9cya.com", true);

        isCertifiableDomain(false, "127.0.0.1", false);
        isCertifiableDomain(false, "::1", false);
        isCertifiableDomain(false, "127.0.0.1", true);
        isCertifiableDomain(false, "::1", true);

    }

    @Test
    public void testFinancial() {
        isCertifiableDomain(false, "google.com", true);
        isCertifiableDomain(false, "twitter.com", true);
    }

    private void isCertifiableDomain(boolean b, String string, boolean puny) {
        try {
            DomainAssessment.checkCertifiableDomain(string, puny, true);
            assertTrue(b);
        } catch (GigiApiException e) {
            assertFalse(e.getMessage(), b);
        }
    }

}
