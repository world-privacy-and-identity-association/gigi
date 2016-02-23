package org.cacert.gigi;

import static org.junit.Assert.*;

import org.cacert.gigi.dbObjects.Domain;
import org.junit.Test;

public class DomainVerification {

    @Test
    public void testDomainPart() {
        assertTrue(Domain.isVaildDomainPart("cacert", false));
        assertTrue(Domain.isVaildDomainPart("de", false));
        assertTrue(Domain.isVaildDomainPart("ha2-a", false));
        assertTrue(Domain.isVaildDomainPart("ha2--a", false));
        assertTrue(Domain.isVaildDomainPart("h--a", false));
        assertFalse(Domain.isVaildDomainPart("xn--bla", false));
        assertFalse(Domain.isVaildDomainPart("-xnbla", false));
        assertFalse(Domain.isVaildDomainPart("xnbla-", false));
        assertFalse(Domain.isVaildDomainPart("", false));
        assertTrue(Domain.isVaildDomainPart("2xnbla", false));
        assertTrue(Domain.isVaildDomainPart("xnbla2", false));
        assertTrue(Domain.isVaildDomainPart("123", false));
        assertTrue(Domain.isVaildDomainPart("abcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxy1234567890123", false));
        assertFalse(Domain.isVaildDomainPart("abcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxy12345678901234", false));
    }

    @Test
    public void testDomainCertifyable() {
        isCertifyableDomain(true, "cacert.org", false);
        isCertifyableDomain(true, "cacert.de", false);
        isCertifyableDomain(true, "cacert.org", false);
        isCertifyableDomain(true, "cacert.org", false);
        isCertifyableDomain(true, "1234.org", false);
        isCertifyableDomain(false, "a.cacert.org", true);
        isCertifyableDomain(false, "gigi.local", true);
        isCertifyableDomain(false, "org", true);
        isCertifyableDomain(false, "'a.org", true);
        isCertifyableDomain(false, ".org", true);
        isCertifyableDomain(false, ".org.", true);
        // non-real-punycode
        isCertifyableDomain(true, "xna-ae.de", false);
        isCertifyableDomain(true, "xn-aae.de", false);

        // illegal punycode:
        // illegal ace prefix
        isCertifyableDomain(false, "aa--b.com", true);
        isCertifyableDomain(false, "xm--ae-a.de", true);

        // illegal punycode content
        isCertifyableDomain(false, "xn--ae-a.com", true);
        isCertifyableDomain(false, "xn--ae.de", true);
        isCertifyableDomain(false, "xn--ae-a.org", true);
        isCertifyableDomain(false, "xn--ae-a.de", true);
        // valid punycode requires permission
        isCertifyableDomain(true, "xn--4ca0bs.de", true);
        isCertifyableDomain(false, "xn--4ca0bs.de", false);
        isCertifyableDomain(true, "xn--a-zfa9cya.de", true);
        isCertifyableDomain(false, "xn--a-zfa9cya.de", false);

        // valid punycode does not help under .com
        isCertifyableDomain(false, "xn--a-zfa9cya.com", true);
        isCertifyableDomain(true, "zfa9cya.com", true);

        isCertifyableDomain(false, "127.0.0.1", false);
        isCertifyableDomain(false, "::1", false);
        isCertifyableDomain(false, "127.0.0.1", true);
        isCertifyableDomain(false, "::1", true);

    }

    private void isCertifyableDomain(boolean b, String string, boolean puny) {
        try {
            Domain.checkCertifyableDomain(string, puny);
            assertTrue(b);
        } catch (GigiApiException e) {
            assertFalse(e.getMessage(), b);
        }
    }

}
