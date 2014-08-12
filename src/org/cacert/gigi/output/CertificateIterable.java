package org.cacert.gigi.output;

import java.sql.SQLException;
import java.util.Map;

import org.cacert.gigi.Certificate;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;

public class CertificateIterable implements IterableDataset {

    Certificate[] certificates;

    public CertificateIterable(Certificate[] certificates) {
        this.certificates = certificates;
    }

    int i = 0;

    @Override
    public boolean next(Language l, Map<String, Object> vars) {
        if (i >= certificates.length) {
            return false;
        }
        Certificate c = certificates[i++];
        try {
            vars.put("state", l.getTranslation(c.getStatus().toString().toLowerCase()));
        } catch (SQLException e) {
            vars.put("state", "Failed");
            e.printStackTrace();
        }
        vars.put("CN", c.getDistinguishedName());
        vars.put("serial", c.getSerial());
        vars.put("digest", c.getMessageDigest());
        vars.put("profile", c.getProfile().getVisibleName());

        vars.put("issued", "TODO"); // TODO output dates
        vars.put("revoked", "TODO");
        vars.put("expire", "TODO");
        return true;
    }
}
