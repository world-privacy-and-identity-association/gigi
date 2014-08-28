package org.cacert.gigi.testUtils;

import java.io.IOException;

public abstract class ClientTest extends ManagedTest {

    protected String email = createUniqueName() + "@example.org";

    protected int userid = createVerifiedUser("a", "b", email, TEST_PASSWORD);

    protected String cookie;

    protected String csrf;

    public ClientTest() {
        try {
            cookie = login(email, TEST_PASSWORD);
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
