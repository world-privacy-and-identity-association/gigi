package club.wpia.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.util.HTMLEncoder;

public class GroupSelector implements Outputable {

    private final String name;

    private Group value = null;

    private final boolean bySupporter;

    public GroupSelector(String name, boolean bySupporter) {
        this.name = HTMLEncoder.encodeHTML(name);
        this.bySupporter = bySupporter;
    }

    public void update(HttpServletRequest r) throws GigiApiException {
        String vS = r.getParameter(name);
        if (vS == null) {
            throw new GigiApiException("No value for group.");
        }
        try {
            value = Group.getByString(vS);
        } catch (IllegalArgumentException e) {
            throw new GigiApiException("Invalid value for group.");
        }
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.println("<select name='" + name + "'>");
        for (Group g : Group.values()) {
            if (mayManage(g)) {
                out.print("<option value='" + g.getDBName());
                if (g.equals(value)) {
                    out.print(" selected");
                }
                out.println("'>");
                g.getName().output(out, l, vars);
                out.println("</option>");
            }
        }
        out.println("</select>");
    }

    private boolean mayManage(Group g) {
        return (bySupporter && g.isManagedBySupport()) || ( !bySupporter && g.isManagedByUser());
    }

    public Group getGroup() {
        return value;
    }
}
