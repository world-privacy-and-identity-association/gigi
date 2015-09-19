package org.cacert.gigi.ping;

import java.io.IOException;

import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.email.MailProbe;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.RandomToken;

public class EmailPinger extends DomainPinger {

    @Override
    public void ping(Domain domain, String configuration, User u, int confId) {
        String mail = configuration + "@" + domain.getSuffix();
        String token = RandomToken.generateToken(16);
        try {
            enterPingResult(confId, PING_STILL_PENDING, "", token);
            MailProbe.sendMailProbe(Language.getInstance(u.getPreferredLocale()), "domain", domain.getId(), token, mail);
        } catch (IOException e) {
            e.printStackTrace();
            updatePingResult(confId, "error", "Mail connection interrupted", token);
        }
    }

}
