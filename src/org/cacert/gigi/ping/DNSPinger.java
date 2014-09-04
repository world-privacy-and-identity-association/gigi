package org.cacert.gigi.ping;

import java.util.Arrays;
import java.util.List;

import javax.naming.NamingException;

import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.util.DNSUtil;

public class DNSPinger extends DomainPinger {

    @Override
    public String ping(Domain domain, String expToken, User u) {
        String[] tokenParts = expToken.split(":", 2);
        List<String> nameservers;
        try {
            nameservers = Arrays.asList(DNSUtil.getNSNames(domain.getSuffix()));
        } catch (NamingException e) {
            return "No authorative nameserver found.";
        }
        StringBuffer result = new StringBuffer();
        result.append("failed: ");
        boolean failed = nameservers.isEmpty();
        nameservers:
        for (String NS : nameservers) {
            boolean found = false;
            try {
                for (String token : DNSUtil.getTXTEntries(tokenParts[0] + "._cacert._auth." + domain.getSuffix(), NS)) {
                    if (token.isEmpty()) {
                        continue;
                    }
                    found = true;
                    if (token.equals(tokenParts[1])) {
                        continue nameservers;
                    }
                }
            } catch (NamingException e) {
                found = false;
            }
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
    }
}
