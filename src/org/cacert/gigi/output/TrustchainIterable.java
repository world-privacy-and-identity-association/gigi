package org.cacert.gigi.output;

import java.util.Map;

import org.cacert.gigi.dbObjects.CACertificate;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;

public class TrustchainIterable implements IterableDataset {

    CACertificate cert;

    public TrustchainIterable(CACertificate cert) {
        this.cert = cert;
    }

    @Override
    public boolean next(Language l, Map<String, Object> vars) {
        if (cert == null) {
            return false;
        }
        vars.put("name", cert.getKeyname());
        vars.put("link", cert.getLink());
        if (cert.isSelfsigned()) {
            cert = null;
            return true;
        }
        cert = cert.getParent();
        return true;
    }
}
