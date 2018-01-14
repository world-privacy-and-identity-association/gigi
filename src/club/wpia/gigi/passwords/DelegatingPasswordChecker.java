package club.wpia.gigi.passwords;

import club.wpia.gigi.GigiApiException;

/**
 * A {@link PasswordChecker} that delegates checks to several other PasswordCheckers
 * and merges their error messages.
 */
public class DelegatingPasswordChecker implements PasswordChecker {

    private final PasswordChecker[] checkers;

    public DelegatingPasswordChecker(PasswordChecker[] checkers) {
        this.checkers = checkers;
    }

    @Override
    public GigiApiException checkPassword(String password, String[] nameParts, String email) {
        GigiApiException exception = new GigiApiException();
        for (PasswordChecker checker : checkers) {
            GigiApiException currentException = checker.checkPassword(password, nameParts, email);
            if (currentException != null) {
                exception.mergeInto(currentException);
            }
        }
        if (exception.isEmpty()) {
            return null;
        } else {
            return exception;
        }
    }
}
