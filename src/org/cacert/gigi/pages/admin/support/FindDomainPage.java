package org.cacert.gigi.pages.admin.support;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.pages.OneFormPage;

public class FindDomainPage extends OneFormPage {

    public static final String PATH = "/support/find/domain";

    public FindDomainPage(String title) {
        super(title, FindDomainForm.class);
    }

    @Override
    public String getSuccessPath(Form f) {
        return SupportUserDetailsPage.PATH + ((FindDomainForm) f).getUserId();
    }

    @Override
    public boolean isPermitted(User u) {
        if (u == null) {
            return false;
        }
        return u.isInGroup(Group.SUPPORTER);
    }
}
