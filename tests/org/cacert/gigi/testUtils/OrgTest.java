package org.cacert.gigi.testUtils;

import java.io.IOException;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Country;
import org.cacert.gigi.dbObjects.Country.CountryCodeType;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Organisation;

public class OrgTest extends ClientTest {

    public OrgTest() throws IOException {
        makeAssurer(u.getId());
        u.grantGroup(u, Group.ORGASSURER);
        clearCaches();
        cookie = login(email, TEST_PASSWORD);
    }

    public Organisation createUniqueOrg() throws GigiApiException {
        Organisation o1 = new Organisation(createUniqueName(), Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS), "pr", "city", "test@example.com", "", "", u);
        return o1;
    }
}
