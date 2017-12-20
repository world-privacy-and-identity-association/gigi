package club.wpia.gigi.ping;

import java.util.Arrays;
import java.util.List;

import javax.naming.NamingException;

import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.DomainPingConfiguration;
import club.wpia.gigi.dbObjects.DomainPingExecution;
import club.wpia.gigi.util.DNSUtil;
import club.wpia.gigi.util.SystemKeywords;

public class DNSPinger extends DomainPinger {

    @Override
    public DomainPingExecution ping(Domain domain, String expToken, CertificateOwner u, DomainPingConfiguration conf) {
        String[] tokenParts = expToken.split(":", 2);
        List<String> nameservers;
        try {
            nameservers = Arrays.asList(DNSUtil.getNSNames(domain.getSuffix()));
        } catch (NamingException e) {
            return enterPingResult(conf, "error", "No authorative nameserver found.", null);
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
            return enterPingResult(conf, PING_SUCCEDED, "", null);
        } else {
            return enterPingResult(conf, "error", result.toString(), null);
        }
    }
}
