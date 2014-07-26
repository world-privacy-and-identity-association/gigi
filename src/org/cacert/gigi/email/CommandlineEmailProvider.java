package org.cacert.gigi.email;

import java.io.IOException;
import java.util.Properties;

public class CommandlineEmailProvider extends EmailProvider {

    public CommandlineEmailProvider(Properties p) {}

    @Override
    public void sendmail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
        synchronized (System.out) {
            System.out.println("== MAIL ==");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println("From: " + from);
            System.out.println("Errors-To: " + errorsto);
            System.out.println("Extra: " + extra);
            System.out.println(message);
        }

    }

    @Override
    public String checkEmailServer(int forUid, String address) throws IOException {
        System.out.println("checkMailBox: " + address);
        return OK;
    }

}
