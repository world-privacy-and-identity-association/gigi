package org.cacert.gigi;

import org.cacert.gigi.dbObjects.User;


public interface PermissionCheckable {

    public boolean isPermitted(User u);

}
