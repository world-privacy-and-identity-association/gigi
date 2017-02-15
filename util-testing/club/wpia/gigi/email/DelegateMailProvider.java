package club.wpia.gigi.email;

import java.io.IOException;
import java.security.Key;
import java.security.cert.Certificate;
import java.util.Properties;

import club.wpia.gigi.email.EmailProvider;

public abstract class DelegateMailProvider extends EmailProvider {

    private EmailProvider target;

    public DelegateMailProvider(Properties props, String name) {
        try {
            if (name != null) {
                Class<?> c = Class.forName(name);
                target = (EmailProvider) c.getDeclaredConstructor(Properties.class).newInstance(props);
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public EmailProvider getTarget() {
        return target;
    }

    @Override
    public void sendMail(String to, String subject, String message, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
        if (target != null) {
            target.sendMail(to, subject, message, replyto, toname, fromname, errorsto, extra);
        }
    }

    @Override
    public String checkEmailServer(int forUid, String address) throws IOException {
        if (target != null) {
            return target.checkEmailServer(forUid, address);
        }
        return OK;
    }

    @Override
    protected void init(Certificate c, Key k) {
        super.init(c, k);
        if (target != null) {
            target.init(c, k);
        }
    }
}
