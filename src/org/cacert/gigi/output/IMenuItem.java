package org.cacert.gigi.output;

import org.cacert.gigi.PermissionCheckable;

/**
 * Markerinterface for an {@link Outputable} speicially used in a {@link Menu}.
 * 
 * @author janis
 */
public interface IMenuItem extends Outputable, PermissionCheckable {
}
