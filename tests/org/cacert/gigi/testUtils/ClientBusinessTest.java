package org.cacert.gigi.testUtils;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.User;

public class ClientBusinessTest extends BusinessTest {

    protected final User u;

    protected final int id;

    public ClientBusinessTest() {
        try {
            id = createVerifiedUser("a", "b", createUniqueName() + "@example.com", TEST_PASSWORD);
            u = User.getById(id);
        } catch (GigiApiException e) {
            throw new Error(e);
        }
    }
}
