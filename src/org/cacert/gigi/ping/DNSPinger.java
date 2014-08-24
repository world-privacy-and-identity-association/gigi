package org.cacert.gigi.ping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;

public class DNSPinger extends DomainPinger {

    @Override
    public String ping(String domain, String expToken) {
        try {
            String[] tokenParts = expToken.split(":", 2);

            Process p = Runtime.getRuntime().exec(new String[] {
                    "dig", "+short", "NS", domain
            });
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            LinkedList<String> nameservers = new LinkedList<String>();
            while ((line = br.readLine()) != null) {
                nameservers.add(line);
            }
            p.destroy();
            StringBuffer result = new StringBuffer();
            result.append("failed: ");
            boolean failed = nameservers.isEmpty();
            nameservers:
            for (String NS : nameservers) {
                String[] call = new String[] {
                        "dig", "@" + NS, "+short", "TXT", "cacert-" + tokenParts[0] + "." + domain
                };
                System.out.println(Arrays.toString(call));
                p = Runtime.getRuntime().exec(call);
                br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String token = null;
                boolean found = false;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    found = true;
                    token = line.substring(1, line.length() - 1);
                    if (token.equals(tokenParts[1])) {
                        continue nameservers;
                    }
                }
                p.destroy();
                result.append(NS);
                if (found) {
                    result.append(" DIFFER;");
                } else {
                    result.append(" EMPTY;");
                }
                failed = true;

            }
            if ( !failed) {
                return PING_SUCCEDED;
            }
            return result.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "Connection closed";
        }
    }

}
