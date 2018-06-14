package club.wpia.gigi.passwords;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

import org.junit.Test;

import club.wpia.gigi.passwords.PasswordChecker;
import club.wpia.gigi.passwords.PasswordHashChecker;
import club.wpia.gigi.testUtils.ClientBusinessTest;

public class TestPasswordHashChecker extends ClientBusinessTest {

    @Test
    public void testPasswordHashes() throws IOException, NoSuchAlgorithmException {
        Path tempPath = Files.createTempFile("hashes", ".bin");
        FileChannel tempFile = FileChannel.open(tempPath, StandardOpenOption.READ, StandardOpenOption.WRITE);
        tempFile.write(ByteBuffer.wrap(new byte[] { (byte)0x5b, (byte)0xaa, (byte)0x61, (byte)0xe4, (byte)0xc9, (byte)0xb9, (byte)0x3f, (byte)0x3f, (byte)0x06, (byte)0x82, (byte)0x25, (byte)0x0b, (byte)0x6c, (byte)0xf8, (byte)0x33, (byte)0x1b, (byte)0x7e, (byte)0xe6, (byte)0x8f, (byte)0xd8 })); // hash of "password"
        tempFile.write(ByteBuffer.wrap(new byte[] { (byte)0x8b, (byte)0xe3, (byte)0xc9, (byte)0x43, (byte)0xb1, (byte)0x60, (byte)0x9f, (byte)0xff, (byte)0xbf, (byte)0xc5, (byte)0x1a, (byte)0xad, (byte)0x66, (byte)0x6d, (byte)0x0a, (byte)0x04, (byte)0xad, (byte)0xf8, (byte)0x3c, (byte)0x9d })); // hash of "Password"
        tempFile.write(ByteBuffer.wrap(new byte[] { (byte)0xab, (byte)0xf7, (byte)0xaa, (byte)0xd6, (byte)0x43, (byte)0x88, (byte)0x36, (byte)0xdb, (byte)0xe5, (byte)0x26, (byte)0xaa, (byte)0x23, (byte)0x1a, (byte)0xbd, (byte)0xe2, (byte)0xd0, (byte)0xee, (byte)0xf7, (byte)0x4d, (byte)0x42 })); // hash of "correct horse battery staple"

        PasswordChecker checker = new PasswordHashChecker(tempFile, MessageDigest.getInstance("SHA-1"));

        assertNotNull(checker.checkPassword("password", new String[0], ""));
        assertNotNull(checker.checkPassword("Password", new String[0], ""));
        assertNotNull(checker.checkPassword("correct horse battery staple", new String[0], ""));
        assertNull(checker.checkPassword("Correct Horse Battery Staple", new String[0], ""));
    }

}
