package club.wpia.gigi.ping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.util.SystemKeywords;

public class HTTPFetch extends DomainPinger {

    @Override
    public void ping(Domain domain, String expToken, CertificateOwner user, int confId) {
        try {
            String[] tokenParts = expToken.split(":", 2);
            URL u = new URL("http://" + domain.getSuffix() + "/" + SystemKeywords.HTTP_CHALLENGE_PREFIX + tokenParts[0] + ".txt");
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            if (huc.getResponseCode() != 200) {
                enterPingResult(confId, "error", "Invalid status code " + huc.getResponseCode() + ".", null);
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(huc.getInputStream(), "UTF-8"));
            String line = br.readLine();
            if (line == null) {
                enterPingResult(confId, "error", "Empty document.", null);
                return;
            }
            if (line.trim().equals(tokenParts[1])) {
                enterPingResult(confId, PING_SUCCEDED, "", null);
                return;
            }
            enterPingResult(confId, "error", "Challenge tokens differed.", null);
            return;
        } catch (IOException e) {
            e.printStackTrace();
            enterPingResult(confId, "error", "Exception: connection closed.", null);
            return;
        }
    }
}
