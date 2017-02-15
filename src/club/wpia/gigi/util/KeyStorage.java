package club.wpia.gigi.util;

import java.io.File;

public class KeyStorage {

    private static final File csr = new File("keys/csr");

    private static final File crt = new File("keys/crt");

    public static File locateCrt(int id) {
        File parent = new File(crt, (id / 1000) + "");
        if ( !parent.exists() && !parent.mkdirs()) {
            throw new Error("cert folder could not be created");
        }
        return new File(parent, id + ".crt");
    }

    public static File locateCsr(int id) {
        File parent = new File(csr, (id / 1000) + "");
        if ( !parent.exists() && !parent.mkdirs()) {
            throw new Error("csr folder could not be created");
        }
        return new File(parent, id + ".csr");
    }
}
