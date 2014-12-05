package org.cacert.gigi.testUtils;

import org.cacert.gigi.dbObjects.User;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RegisteredUser implements TestRule {

    User u;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                u = User.getById(ManagedTest.createVerifiedUser("fn", "ln", ManagedTest.createUniqueName() + "@example.org", ManagedTest.TEST_PASSWORD));
                try {
                    base.evaluate();
                } finally {

                }
            }
        };
    }

    public User getUser() {
        return u;
    }

}
