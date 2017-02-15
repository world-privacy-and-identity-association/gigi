package club.wpia.gigi.output;

import club.wpia.gigi.PermissionCheckable;
import club.wpia.gigi.output.template.Outputable;

/**
 * Markerinterface for an {@link Outputable} speicially used in a {@link Menu}.
 * 
 * @author janis
 */
public interface IMenuItem extends Outputable, PermissionCheckable {
}
