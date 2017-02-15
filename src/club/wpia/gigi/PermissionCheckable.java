package club.wpia.gigi;

import club.wpia.gigi.util.AuthorizationContext;

public interface PermissionCheckable {

    public boolean isPermitted(AuthorizationContext u);

}
