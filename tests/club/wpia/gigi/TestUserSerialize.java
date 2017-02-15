package club.wpia.gigi;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.BusinessTest;

public class TestUserSerialize extends BusinessTest {

    private byte[] serialize(Object o) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(o);
                oos.flush();
            }
            baos.flush();
            return baos.toByteArray();
        }
    }

    private Object deserialize(byte[] ba) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(ba)) {
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                Object o = ois.readObject();
                return o;
            }
        }
    }

    @Test
    public void testSerializeUser() throws GigiApiException, IOException, ClassNotFoundException {
        User u = createVerifiedUser();
        byte[] ba = serialize(u);
        Object uo = deserialize(ba);
        assertSame("Original user and the deserialized object must be the same", u, uo);
    }

}
