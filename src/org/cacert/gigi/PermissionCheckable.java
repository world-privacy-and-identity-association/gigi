package org.cacert.gigi;

import org.cacert.gigi.util.AuthorizationContext;

public interface PermissionCheckable {

    public boolean isPermitted(AuthorizationContext u);

}
