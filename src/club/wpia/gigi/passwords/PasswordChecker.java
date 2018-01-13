package club.wpia.gigi.passwords;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Name;

/**
 * A strategy to check whether a password is acceptable for use or not.
 */
public interface PasswordChecker {

    /**
     * Checks if a password is acceptable for use.
     * Most implementations judge a password’s strength in some way
     * and reject weak passwords.
     *
     * @param password The password to check.
     * @param nameParts The name parts of the user that wants to use this password.
     * @param email The email address of the user that wants to use this password.
     * @return {@code null} if the password is acceptable,
     * otherwise a {@link GigiApiException} with an appropriate error message.
     */
    public GigiApiException checkPassword(String password, String[] nameParts, String email);
}
