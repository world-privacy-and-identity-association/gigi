package club.wpia.gigi.output;

import java.util.Map;

import club.wpia.gigi.dbObjects.CACertificate;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.IterableDataset;

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
