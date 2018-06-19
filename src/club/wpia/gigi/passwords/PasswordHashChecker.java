package club.wpia.gigi.passwords;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Arrays;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.output.template.SprintfCommand;

/**
 * A {@link PasswordChecker} which searches for a password
 * in a given file containing hashes of known breached passwords.
 * If the password is found, it is rejected.
 */
public class PasswordHashChecker implements PasswordChecker {

    private final FileChannel database;

    private final MessageDigest digest;

    private final int digestLength;

    private final ByteBuffer hashBuffer;

    /**
     * @param database The database of password hashes.
     *                 It should contain the hashes in binary format, with no separating characters,
     *                 sorted in ascending order.
     * @param digest   The digest function that was used to compile the database.
     */
    public PasswordHashChecker(FileChannel database, MessageDigest digest) {
        this.database = database;
        this.digest = digest;
        this.digestLength = digest.getDigestLength();
        this.hashBuffer = ByteBuffer.allocate(digestLength);
    }

    @Override
    public GigiApiException checkPassword(String password, String[] nameParts, String email) {
        byte[] passwordHash = passwordHash(password);
        boolean knownPasswordHash;
        try {
            knownPasswordHash = knownPasswordHash(passwordHash);
        } catch (IOException e) {
            System.err.printf(
                "Error while reading password hash database, ACCEPTING POTENTIALLY KNOWN PASSWORD for user with email %s.",
                email
            );
            e.printStackTrace();
            knownPasswordHash = false;
        }
        if (knownPasswordHash) {
            return new GigiApiException(new SprintfCommand(
                "The password you submitted was found in a public database of passwords exposed in data breaches. If you use this password anywhere else, you should change it. See our {0}FAQ{1} for more information.",
                Arrays.asList("!(/kb/knownPasswordHash", "!'</a>'")
            ));
        } else {
            return null;
        }
    }

    private byte[] passwordHash(String password) {
        byte[] passwordBytes = password.getBytes(Charset.forName("UTF-8"));
        byte[] passwordHash;
        synchronized (digest) {
            digest.reset();
            digest.update(passwordBytes);
            passwordHash = digest.digest();
        }
        return passwordHash;
    }

    private boolean knownPasswordHash(byte[] passwordHash) throws IOException {
        long targetEstimate = estimateHashOffset(passwordHash);
        long bestGuess = targetEstimate;
        bestGuess = clampOffset(bestGuess);

        hashBuffer.clear();
        database.read(hashBuffer, bestGuess);
        // first, guess the location of the hash within the file
        for (int i = 0; i < 4; i++) {
            long bestGuessEstimate = estimateHashOffset(hashBuffer.array());
            if (bestGuessEstimate == targetEstimate) {
                break;
            }
            bestGuess = bestGuess + targetEstimate - bestGuessEstimate;
            bestGuess = clampOffset(bestGuess);
            hashBuffer.clear();
            database.read(hashBuffer, bestGuess);
        }
        int searchDirection = compareHashes(passwordHash, hashBuffer.array());
        if (searchDirection == 0) {
            return true;
        }
        // then, do a linear search from that location until we’ve either found or moved past the hash
        int newSearchDirection = searchDirection;
        while (searchDirection == newSearchDirection) {
            bestGuess += digestLength * searchDirection;
            if (bestGuess < 0 || bestGuess >= database.size()) {
                break;
            }
            hashBuffer.clear();
            database.read(hashBuffer, bestGuess);
            newSearchDirection = compareHashes(passwordHash, hashBuffer.array());
        }
        return newSearchDirection == 0;
    }

    private int compareHashes(byte[] left, byte[] right) {
        for (int i = 0; i < digestLength; i++) {
            int leftByte = Byte.toUnsignedInt(left[i]);
            int rightByte = Byte.toUnsignedInt(right[i]);
            if (leftByte < rightByte) {
                return -1;
            }
            if (leftByte > rightByte) {
                return 1;
            }
        }
        return 0;
    }

    private long estimateHashOffset(byte[] hash) throws IOException {
        long pos = (database.size()
                * ((hash[0] & 0xFF) << 8 | (hash[1] & 0xFF)))
                / (1L << 16);
        pos += (database.size()
                * ((hash[2] & 0xFF) << 8 | (hash[3] & 0xFF)))
                / (1L << 32);
        return (pos / digestLength) * digestLength;
    }

    private long clampOffset(long offset) throws IOException {
        if (offset < 0) {
            return 0;
        }
        if (offset >= database.size()) {
            return database.size() - 1;
        }
        return offset;
    }
}
