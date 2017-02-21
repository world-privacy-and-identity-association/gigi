package club.wpia.gigi.email;

import java.io.IOException;
import java.util.HashMap;

import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.MailTemplate;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class MailProbe {

    private static final MailTemplate mailProbe = new MailTemplate(MailProbe.class.getResource("MailProbe.templ"));

    public static void sendMailProbe(Language l, String type, int id, String hash, String address) throws IOException {
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("link", "https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + "/verify?type=" + type + "&id=" + id + "&hash=" + hash);
        vars.put("appName", ServerConstants.getAppName());
        mailProbe.sendMail(l, vars, address);
    }

}
