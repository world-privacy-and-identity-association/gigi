package org.cacert.gigi.dbObjects;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.template.SprintfCommand;

public class CountryCode {

    public enum CountryCodeType {
        CODE_2_CHARS(2, //
                "SELECT `id`, `english` as country, `code2` as countrycode FROM `countryIsoCode` ORDER BY code2",//
                "SELECT `id`, `english` as country, `code2` as countrycode FROM `countryIsoCode` WHERE `code2`=?",//
                "SELECT 1 FROM `countryIsoCode` WHERE `code2`=?"),//
        CODE_3_CHARS(3,//
                "SELECT `id`, `english` as country, `code3` as countrycode FROM `countryIsoCode` ORDER BY code3", //
                "SELECT `id`, `english` as country, `code3` as countrycode FROM `countryIsoCode` WHERE `code3`=?",//
                "SELECT 1 FROM `countryIsoCode` WHERE `code3`=?");

        private final String listQuery;

        private final String getQuery;

        private final String validationQuery;

        private final int len;

        private CountryCodeType(int len, String listQuery, String getQuery, String validationQuery) {
            this.len = len;
            this.listQuery = listQuery;
            this.getQuery = getQuery;
            this.validationQuery = validationQuery;
        }

        public int getLen() {
            return len;
        }

        public String getGetQuery() {
            return getQuery;
        }

        public String getListQuery() {
            return listQuery;
        }

        public String getValidationQuery() {
            return validationQuery;
        }
    }

    private final int id;

    private final String country;

    private final String countryCode;

    public CountryCode(int id, String country, String countryCode) {
        this.id = id;
        this.country = country;
        this.countryCode = countryCode;
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

    public static CountryCode[] getCountryCodes(CountryCodeType clength) throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement(clength.getListQuery(), true)) {
            GigiResultSet rs = ps.executeQuery();

            rs.last();
            int totalCount = rs.getRow();
            rs.beforeFirst();
            int i = 0;

            CountryCode[] finalResult = new CountryCode[totalCount];
            while (rs.next()) {
                finalResult[i] = new CountryCode(rs.getInt("id"), rs.getString("country"), rs.getString("countrycode"));
                i += 1;
            }

            return finalResult;
        }
    }

    public static void checkCountryCode(String countrycode, CountryCodeType cType) throws GigiApiException {
        if (countrycode.length() != cType.getLen()) {
            throw new GigiApiException(SprintfCommand.createSimple("Country code length does not have the required length of {0} characters", Integer.toString(cType.getLen())));
        }
        try (GigiPreparedStatement ps = new GigiPreparedStatement(cType.getValidationQuery())) {
            ps.setString(1, countrycode.toUpperCase());
            GigiResultSet rs = ps.executeQuery();

            if ( !rs.next()) {
                throw new GigiApiException(SprintfCommand.createSimple("Country code {0} is not available in database", countrycode.toUpperCase()));
            }
        }

    }

    public static CountryCode getCountryCode(String countrycode, CountryCodeType cType) throws GigiApiException {
        if (countrycode.length() != cType.getLen()) {
            throw new GigiApiException(SprintfCommand.createSimple("Country code length does not have the required length of {0} characters", Integer.toString(cType.getLen())));
        }
        try (GigiPreparedStatement ps = new GigiPreparedStatement(cType.getGetQuery())) {
            ps.setString(1, countrycode.toUpperCase());
            GigiResultSet rs = ps.executeQuery();

            if ( !rs.next()) {
                throw new GigiApiException(SprintfCommand.createSimple("Country code {0} is not available in database", countrycode.toUpperCase()));
            }
            return new CountryCode(rs.getInt("id"), rs.getString("country"), rs.getString("countrycode"));
        }

    }
}
