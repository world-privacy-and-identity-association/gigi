package org.cacert.gigi;


public interface PermissionCheckable {

    public boolean isPermitted(User u);

}
