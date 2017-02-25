package club.wpia.gigi.testUtils;

import java.io.IOException;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.Country.CountryCodeType;

public class OrgTest extends ClientTest {

    public OrgTest() throws IOException, GigiApiException {
        makeAgent(u.getId());
        u.grantGroup(getSupporter(), Group.ORG_AGENT);
        clearCaches();
        cookie = login(email, TEST_PASSWORD);
    }

    public Organisation createUniqueOrg() throws GigiApiException {
        Organisation o1 = new Organisation(createUniqueName(), Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS), "pr", "city", "test@example.com", "", "", u);
        return o1;
    }
}
