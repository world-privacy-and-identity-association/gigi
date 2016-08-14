package org.cacert.gigi.dbObjects;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.template.SprintfCommand;

public class CountryCode {

    public enum CountryCodeType {
        CODE_2_CHARS(2, //
                "SELECT `id`, `english` as country, `code2` as countrycode FROM `countryIsoCode` ORDER BY code2"), //
        CODE_3_CHARS(3,//
                "SELECT `id`, `english` as country, `code3` as countrycode FROM `countryIsoCode` ORDER BY code3"); //

        private final String listQuery;

        private final int len;

        private CountryCodeType(int len, String listQuery) {
            this.len = len;
            this.listQuery = listQuery;
        }

        public int getLen() {
            return len;
        }

        protected String getListQuery() {
            return listQuery;
        }
    }

    private final int id;

    private final String country;

    private final String countryCode;

    private final CountryCodeType ctype;

    private static final CountryCode[] c2s;

    private static final CountryCode[] c3s;

    private static final Map<String, CountryCode> byString;
    static {
        try {
            c2s = getCountryCodesFromDB(CountryCodeType.CODE_2_CHARS);
            c3s = getCountryCodesFromDB(CountryCodeType.CODE_3_CHARS);
            HashMap<String, CountryCode> ccd = new HashMap<>();
            for (CountryCode c2 : c2s) {
                ccd.put(c2.getCountryCode(), c2);
            }
            for (CountryCode c3 : c3s) {
                ccd.put(c3.getCountryCode(), c3);
            }
            byString = Collections.unmodifiableMap(ccd);
        } catch (GigiApiException e) {
            throw new Error(e);
        }
    }

    private CountryCode(int id, String country, String countryCode, CountryCodeType ctype) {
        this.id = id;
        this.country = country;
        this.countryCode = countryCode;
        this.ctype = ctype;
    }

    public int getId() {
        return id;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public CountryCodeType getCountryCodeType() {
        return ctype;
    }

    public static CountryCode[] getCountryCodes(CountryCodeType clength) {
        switch (clength) {
        case CODE_2_CHARS:
            return Arrays.copyOf(c2s, c2s.length);
        case CODE_3_CHARS:
            return Arrays.copyOf(c3s, c3s.length);
        }
        throw new Error("Enum switch was not exhaustive.");
    }

    private static CountryCode[] getCountryCodesFromDB(CountryCodeType clength) throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement(clength.getListQuery(), true)) {
            GigiResultSet rs = ps.executeQuery();

            rs.last();
            int totalCount = rs.getRow();
            rs.beforeFirst();
            int i = 0;

            CountryCode[] finalResult = new CountryCode[totalCount];
            while (rs.next()) {
                finalResult[i] = new CountryCode(rs.getInt("id"), rs.getString("country"), rs.getString("countrycode"), clength);
                i += 1;
            }

            return finalResult;
        }
    }

    public static void checkCountryCode(String countrycode, CountryCodeType cType) throws GigiApiException {
        getCountryCode(countrycode, cType);
    }

    public CountryCode convertToCountryCodeType(CountryCodeType ctype) {
        if (this.ctype.equals(ctype)) {
            return this;
        }
        CountryCode[] cclist = getCountryCodes(ctype);
        for (CountryCode cc : cclist) {
            if (cc.getId() == this.getId()) {
                return cc;
            }
        }
        throw new RuntimeException("Internal Error: CountryCode for country not found" + this.getCountry());
    }

    public static CountryCode getCountryCode(String countrycode, CountryCodeType cType) throws GigiApiException {
        if (countrycode.length() != cType.getLen()) {
            throw new GigiApiException(SprintfCommand.createSimple("Country code length does not have the required length of {0} characters", Integer.toString(cType.getLen())));
        }
        CountryCode i = byString.get(countrycode);
        if (i == null || i.getCountryCodeType() != cType) {
            throw new GigiApiException("Country Code was wrong.");
        }
        return i;
    }

    public static CountryCode getRandomCountry(CountryCodeType cType) {
        CountryCode[] cc = CountryCode.getCountryCodes(cType);
        int rnd = new Random().nextInt(cc.length);
        return cc[rnd];
    }

}
