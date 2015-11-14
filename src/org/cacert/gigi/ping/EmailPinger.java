package org.cacert.gigi.ping;

import java.io.IOException;
import java.util.Locale;

import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.email.MailProbe;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.RandomToken;

public class EmailPinger extends DomainPinger {

    @Override
    public void ping(Domain domain, String configuration, CertificateOwner u, int confId) {
        String mail = configuration + "@" + domain.getSuffix();
        String token = RandomToken.generateToken(16);
        try {
            enterPingResult(confId, PING_STILL_PENDING, "", token);
            Locale l = Locale.ENGLISH;
            if (u instanceof User) {
                l = ((User) u).getPreferredLocale();
                // TODO what to do with orgs?
            }
            MailProbe.sendMailProbe(Language.getInstance(l), "domain", domain.getId(), token, mail);
        } catch (IOException e) {
            e.printStackTrace();
            updatePingResult(confId, "error", "Mail connection interrupted", token);
        }
    }

}
