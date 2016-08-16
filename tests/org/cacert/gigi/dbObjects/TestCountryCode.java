package org.cacert.gigi.dbObjects;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Country.CountryCodeType;
import org.cacert.gigi.testUtils.BusinessTest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestCountryCode extends BusinessTest {

    @Parameter(0)
    public CountryCodeType type;

    @Parameters(name = "Type: {0}")
    public static Iterable<Object[]> getParameters() {
        return Arrays.<Object[]>asList(new Object[] {
                CountryCodeType.CODE_2_CHARS
        }, new Object[] {
                CountryCodeType.CODE_3_CHARS
        });
    }

    @Test
    public void testList() throws GigiApiException {
        List<Country> ccs = Country.getCountries();
        for (Country cc : ccs) {
            assertThat(cc.getCode(type), stringLength(type.getLen()));
        }
    }

    @Test
    public void testFetch() throws GigiApiException {
        String ref = type == CountryCodeType.CODE_2_CHARS ? "DE" : "DEU";
        Country cc = Country.getCountryByCode(ref, type);
        assertEquals(ref, cc.getCode(type));
        assertEquals("Germany", cc.getName());
    }

    @Test
    public void testCheck() throws GigiApiException {
        String ref = type == CountryCodeType.CODE_2_CHARS ? "DE" : "DEU";
        String reff = type == CountryCodeType.CODE_2_CHARS ? "DF" : "DFU";

        Country.checkCountryCode(ref, type);
        try {
            Country.checkCountryCode(reff, type);
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("was wrong"));
        }

        Country.getCountryByCode(ref, type);
        try {
            Country.getCountryByCode(reff, type);
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("was wrong"));
        }
    }

    @Test
    public void testSingleInstance() throws GigiApiException {
        String ref = type == CountryCodeType.CODE_2_CHARS ? "DE" : "DEU";
        assertSame(Country.getCountryByCode(ref, type), Country.getCountryByCode(ref, type));
    }

    private Matcher<String> stringLength(final int len) {
        return new BaseMatcher<String>() {

            @Override
            public boolean matches(Object s) {
                if (s instanceof String) {
                    return ((String) s).length() == len;
                }
                return false;
            }

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("String of length " + len);
            }

        };
    }

}
