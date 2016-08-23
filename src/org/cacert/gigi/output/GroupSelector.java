package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.util.HTMLEncoder;

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
        value = null;
        for (Group g : Group.values()) {
            if (g.getDatabaseName().equals(vS) && mayManage(g)) {
                value = g;
            }
        }
        if (value == null) {
            throw new GigiApiException("Invalid value for group.");
        }
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.println("<select name='" + name + "'>");
        for (Group g : Group.values()) {
            if (mayManage(g)) {
                out.print("<option value='" + g.getDatabaseName());
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
