package org.cacert.gigi.testUtils;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.cacert.gigi.dbObjects.User;

/**
 * Superclass for testsuites in a scenario where there is an registered member,
 * who is already logged on.
 */
public abstract class ClientTest extends ManagedTest {

    /**
     * Email of the member.
     */
    protected String email = createUniqueName() + "@example.org";

    /**
     * Id of the member
     */
    protected int id = createVerifiedUser("a", "b", email, TEST_PASSWORD);

    /**
     * {@link User} object of the member
     */
    protected User u = User.getById(id);

    /**
     * Session cookie of the member.
     */
    protected String cookie;

    public ClientTest() {
        try {
            cookie = login(email, TEST_PASSWORD);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public HttpURLConnection post(String path, String query) throws IOException {
        return post(path, query, 0);
    }

    public HttpURLConnection post(String path, String query, int formIndex) throws IOException {
        return post(cookie, path, query, formIndex);
    }

    public HttpURLConnection get(String path) throws IOException {
        return get(cookie, path);
    }

}
