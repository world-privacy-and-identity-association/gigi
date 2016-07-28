package org.cacert.gigi.email;

import java.io.IOException;
import java.util.HashMap;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.MailTemplate;
import org.cacert.gigi.util.ServerConstants;

public class MailProbe {

    private static final MailTemplate mailProbe = new MailTemplate(MailProbe.class.getResource("MailProbe.templ"));

    public static void sendMailProbe(Language l, String type, int id, String hash, String address) throws IOException {
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("link", "https://" + ServerConstants.getWwwHostNamePortSecure() + "/verify?type=" + type + "&id=" + id + "&hash=" + hash);
        mailProbe.sendMail(l, vars, address);
    }

}
