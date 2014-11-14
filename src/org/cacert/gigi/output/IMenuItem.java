package org.cacert.gigi.output;

import org.cacert.gigi.PermissionCheckable;
import org.cacert.gigi.output.template.Outputable;

/**
 * Markerinterface for an {@link Outputable} speicially used in a {@link Menu}.
 * 
 * @author janis
 */
public interface IMenuItem extends Outputable, PermissionCheckable {
}
