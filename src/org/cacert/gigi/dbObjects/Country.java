package org.cacert.gigi.dbObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.RandomAccess;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.template.SprintfCommand;

/**
 * Represents a country containing its ISO 3166-1-Code and its English name.
 */
public class Country {

    public enum CountryCodeType {
        CODE_2_CHARS(2), //
        CODE_3_CHARS(3); //

        private final int len;

        private CountryCodeType(int len) {
            this.len = len;
        }

        public int getLen() {
            return len;
        }
    }

    /**
     * Id of the database entry.
     */
    private final int id;

    /**
     * English name of the country.
     */
    private final String country;

    /**
     * ISO 3166-1 alpha-2 code of the country.
     */
    private final String countryCode2;

    /**
     * ISO 3166-1 alpha-3 code of the country.
     */
    private final String countryCode3;

    /**
     * A unmodifiable {@link RandomAccess}-List of all Countries.
     */
    private static final List<Country> countries;

    /**
     * An unmodifiable index of all 2- and 3-letter country codes.
     */
    private static final Map<String, Country> byString;

    static {
        LinkedList<Country> cs = new LinkedList<>();
        HashMap<String, Country> ccd = new HashMap<>();
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `id`, `english` as country, `code2`, `code3` FROM `countryIsoCode`", true)) {
            GigiResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Country e = new Country(rs);
                ccd.put(e.countryCode2, e);
                ccd.put(e.countryCode3, e);
                cs.add(e);
            }
        }
        countries = Collections.unmodifiableList(new ArrayList<>(cs));
        byString = Collections.unmodifiableMap(ccd);
    }

    private Country(GigiResultSet rs) {
        this.id = rs.getInt("id");
        this.country = rs.getString("country");
        this.countryCode2 = rs.getString("code2");
        this.countryCode3 = rs.getString("code3");
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return country;
    }

    /**
     * Returns the default (ISO 3166-1 alpha-2) country code of this country.
     * 
     * @return the country code
     */
    public String getCode() {
        return countryCode2;
    }

    /**
     * Gets the specified type of country code for this country.
     * 
     * @param type
     *            the type of the code
     * @return the corresponding code
     */
    public String getCode(CountryCodeType type) {
        switch (type) {
        case CODE_2_CHARS:
            return countryCode2;
        case CODE_3_CHARS:
            return countryCode3;
        default:
            throw new IllegalArgumentException("Enum switch was non-exhaustive");
        }
    }

    /**
     * Gets an unmodifiable, {@link RandomAccess}-List of all countries.
     * 
     * @return the list.
     */
    public static List<Country> getCountries() {
        return countries;
    }

    /**
     * Checks a country code for its validity and conformance to the given type.
     * 
     * @param countrycode
     *            the code to check
     * @param cType
     *            the type it should have
     * @throws GigiApiException
     *             if the code was wrong
     */
    public static void checkCountryCode(String countrycode, CountryCodeType cType) throws GigiApiException {
        getCountryByCode(countrycode, cType);
    }

    /**
     * Fetches the {@link Country} object for the given country code.
     * 
     * @param countrycode
     *            the code to fetch the county for
     * @param cType
     *            the type of the code
     * @return the specified country
     * @throws GigiApiException
     *             if the code was wrong.
     */
    public static Country getCountryByCode(String countrycode, CountryCodeType cType) throws GigiApiException {
        if (countrycode.length() != cType.getLen()) {
            throw new GigiApiException(SprintfCommand.createSimple("Country code length does not have the required length of {0} characters", Integer.toString(cType.getLen())));
        }
        Country i = byString.get(countrycode);
        if (i == null) {
            throw new GigiApiException("Country Code was wrong.");
        }
        return i;
    }

    public static Country getRandomCountry() {
        List<Country> cc = Country.getCountries();
        int rnd = new Random().nextInt(cc.size());
        return cc.get(rnd);
    }
}
