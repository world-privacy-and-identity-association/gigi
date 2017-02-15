package club.wpia.gigi.email;

import java.io.IOException;
import java.util.Properties;

import club.wpia.gigi.email.EmailProvider;

public class CommandlineEmailProvider extends EmailProvider {

    public CommandlineEmailProvider(Properties p) {}

    @Override
    public void sendMail(String to, String subject, String message, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
        synchronized (System.out) {
            System.out.println("== MAIL ==");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
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
