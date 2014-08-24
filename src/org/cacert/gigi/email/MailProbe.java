package org.cacert.gigi.email;

import java.io.IOException;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.ServerConstants;

public class MailProbe {

    public static void sendMailProbe(Language l, String type, int id, String hash, String address) throws IOException {
        StringBuffer body = new StringBuffer();
        body.append(l.getTranslation("Thanks for signing up with CAcert.org, below is the link you need to open to verify your account. Once your account is verified you will be able to start issuing certificates till your hearts' content!"));
        body.append("\n\nhttps://");
        body.append(ServerConstants.getWwwHostNamePort());
        body.append("/verify?type=");
        body.append(type);
        body.append("&id=");
        body.append(id);
        body.append("&hash=");
        body.append(hash);
        body.append("\n\n");
        body.append(l.getTranslation("Best regards"));
        body.append("\n");
        body.append(l.getTranslation("CAcert.org Support!"));
        EmailProvider.getInstance().sendmail(address, "[CAcert.org] " + l.getTranslation("Mail Probe"), body.toString(), "support@cacert.org", null, null, null, null, false);
    }

}
