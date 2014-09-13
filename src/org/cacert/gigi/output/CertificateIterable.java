package org.cacert.gigi.output;

import java.util.Map;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;

public class CertificateIterable implements IterableDataset {

    private Certificate[] certificates;

    public CertificateIterable(Certificate[] certificates) {
        this.certificates = certificates;
    }

    private int i = 0;

    @Override
    public boolean next(Language l, Map<String, Object> vars) {
        if (i >= certificates.length) {
            return false;
        }
        Certificate c = certificates[i++];
        vars.put("state", l.getTranslation(c.getStatus().toString().toLowerCase()));
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
