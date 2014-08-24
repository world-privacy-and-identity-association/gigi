package org.cacert.gigi.ping;

import java.io.IOException;
import org.cacert.gigi.Domain;
import org.cacert.gigi.User;
import org.cacert.gigi.email.MailProbe;

public class EmailPinger extends DomainPinger {

    @Override
    public String ping(Domain domain, String configuration, User u) {
        String[] parts = configuration.split(":", 2);
        String mail = parts[0] + "@" + domain.getSuffix();
        try {
            MailProbe.sendMailProbe(u.getPrefferedLanguage(), "domain", domain.getId(), parts[1], mail);
        } catch (IOException e) {
            e.printStackTrace();
            return "Mail connection interrupted";
        }
        return PING_STILL_PENDING;
    }

}
