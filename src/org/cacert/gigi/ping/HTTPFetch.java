package org.cacert.gigi.ping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.User;

public class HTTPFetch extends DomainPinger {

    @Override
    public String ping(Domain domain, String expToken, User user) {
        try {
            String[] tokenParts = expToken.split(":", 2);
            URL u = new URL("http://" + domain.getSuffix() + "/cacert-" + tokenParts[0] + ".txt");
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            if (huc.getResponseCode() != 200) {
                return "Invalid status code.";
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(huc.getInputStream(), "UTF-8"));
            String line = br.readLine();
            if (line == null) {
                return "No response from your server.";
            }
            if (line.trim().equals(tokenParts[1])) {
                return PING_SUCCEDED;
            }
            return "Challange tokens differed.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Connection closed.";
        }
    }
}
