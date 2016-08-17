package org.cacert.gigi.pages.statistics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.ArrayIterable;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;

public class StatisticsRoles extends Page {

    public static final String PATH = "/statistics/roles";

    public StatisticsRoles() {
        super("Statistics Roles");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final User u = getUser(req);
        final boolean supporter = LoginPage.getAuthorizationContext(req).canSupport();

        HashMap<String, Object> vars = new HashMap<String, Object>();

        vars.put("groups", new ArrayIterable<Group>(Group.values()) {

            @Override
            public void apply(Group g, Language l, Map<String, Object> vars) {
                int membersCount = g.getMemberCount();
                vars.put("group_name", g.getName());
                vars.put("count", membersCount);
                if ((supporter || u.isInGroup(g) && g.isSelfViewable()) && g.isManagedBySupport()) {
                    final User[] userg = g.getMembers(0, membersCount);
                    vars.put("memberlist", new ArrayIterable<User>(userg) {

                        @Override
                        public void apply(User userg, Language l, Map<String, Object> vars) {
                            vars.put("name", userg.getPreferredName());
                            vars.put("email", userg.getEmail());
                        }
                    });
                } else {
                    vars.remove("memberlist");
                }
            }

        });

        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

}
