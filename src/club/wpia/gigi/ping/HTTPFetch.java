package club.wpia.gigi.ping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.DomainPingConfiguration;
import club.wpia.gigi.dbObjects.DomainPingExecution;
import club.wpia.gigi.util.SystemKeywords;

public class HTTPFetch extends DomainPinger {

    @Override
    public DomainPingExecution ping(Domain domain, String expToken, CertificateOwner user, DomainPingConfiguration conf) {
        try {
            String[] tokenParts = expToken.split(":", 2);
            URL u = new URL("http://" + domain.getSuffix() + "/" + SystemKeywords.HTTP_CHALLENGE_PREFIX + tokenParts[0] + ".txt");
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            if (huc.getResponseCode() != 200) {
                return enterPingResult(conf, "error", "Invalid status code " + huc.getResponseCode() + ".", null);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(huc.getInputStream(), "UTF-8"));
            String line = br.readLine();
            if (line == null) {
                return enterPingResult(conf, "error", "Empty document.", null);
            }
            if (line.trim().equals(tokenParts[1])) {
                return enterPingResult(conf, PING_SUCCEDED, "", null);
            }
            return enterPingResult(conf, "error", "Challenge tokens differed.", null);
        } catch (IOException e) {
            e.printStackTrace();
            return enterPingResult(conf, "error", "Exception: connection closed.", null);
        }
    }
}
