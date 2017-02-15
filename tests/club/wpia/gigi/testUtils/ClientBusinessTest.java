package club.wpia.gigi.testUtils;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.User;

public class ClientBusinessTest extends BusinessTest {

    protected final User u;

    protected final Name n0;

    protected final int id;

    public ClientBusinessTest() {
        try {
            id = createVerifiedUser("a", "b", createUniqueName() + "@example.com", TEST_PASSWORD);
            u = User.getById(id);
            n0 = u.getNames()[0];
        } catch (GigiApiException e) {
            throw new Error(e);
        }
    }
}
