package org.cacert.gigi.pages.admin.support;

import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.pages.OneFormPage;
import org.cacert.gigi.util.AuthorizationContext;

public class FindUserByDomainPage extends OneFormPage {

    public static final String PATH = "/support/find/domain";

    public FindUserByDomainPage() {
        super("Find Domain", FindUserByDomainForm.class);
    }

    @Override
    public String getSuccessPath(Form f) {
        CertificateOwner res = ((FindUserByDomainForm) f).getRes();
        if (res instanceof User) {
            return SupportUserDetailsPage.PATH + res.getId();
        } else if (res instanceof Organisation) {
            return "/support/domain/" + res.getId();
        } else {
            throw new Error("Unknown owner type.");
        }
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.canSupport();
    }
}
