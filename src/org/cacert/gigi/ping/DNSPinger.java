package org.cacert.gigi.ping;

import java.util.Arrays;
import java.util.List;

import javax.naming.NamingException;

import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.util.DNSUtil;
import org.cacert.gigi.util.SystemKeywords;

public class DNSPinger extends DomainPinger {

    @Override
    public void ping(Domain domain, String expToken, CertificateOwner u, int confId) {
        String[] tokenParts = expToken.split(":", 2);
        List<String> nameservers;
        try {
            nameservers = Arrays.asList(DNSUtil.getNSNames(domain.getSuffix()));
        } catch (NamingException e) {
            enterPingResult(confId, "error", "No authorative nameserver found.", null);
            return;
        }
        StringBuffer result = new StringBuffer();
        result.append("failed: ");
        boolean failed = nameservers.isEmpty();
        nameservers:
        for (String NS : nameservers) {
            boolean found = false;
            try {
                for (String token : DNSUtil.getTXTEntries(tokenParts[0] + "." + SystemKeywords.DNS_PREFIX + "._auth." + domain.getSuffix(), NS)) {
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
            enterPingResult(confId, PING_SUCCEDED, "", null);
        } else {
            enterPingResult(confId, "error", result.toString(), null);
        }
    }
}
