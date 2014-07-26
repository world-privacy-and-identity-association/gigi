package org.cacert.gigi.natives;

import java.io.File;

/**
 * Native to use privileged ports on unix-like hosts.
 * 
 * @author janis
 */
public class SetUID {

    static {
        System.load(new File("natives/libsetuid.so").getAbsolutePath());
    }

    public native Status setUid(int uid, int gid);

    public static class Status {

        private boolean success;

        private String message;

        public Status(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean getSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
