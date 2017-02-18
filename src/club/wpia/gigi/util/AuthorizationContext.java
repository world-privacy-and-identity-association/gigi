package club.wpia.gigi.util;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.output.template.SprintfCommand;

public class AuthorizationContext implements Outputable, Serializable {

    private static final long serialVersionUID = -2596733469159940154L;

    private CertificateOwner target;

    private User actor;

    private String supporterTicketId;

    public AuthorizationContext(CertificateOwner target, User actor) {
        this.target = target;
        this.actor = actor;
    }

    public AuthorizationContext(User actor, String supporterTicket) throws GigiApiException {
        this.target = actor;
        this.actor = actor;
        if ( !isInGroup(Group.SUPPORTER)) {
            throw new GigiApiException("requires a supporter");
        }
        supporterTicketId = supporterTicket;
    }

    public CertificateOwner getTarget() {
        return target;
    }

    public User getActor() {
        return actor;
    }

    public boolean isInGroup(Group g) {
        return actor.isInGroup(g);
    }

    public User getActor(AuthorizationContext ac) {
        if (ac == null) {
            return null;
        }
        return ac.getActor();
    }

    public String getSupporterTicketId() {
        return supporterTicketId;
    }

    public boolean canSupport() {
        return getSupporterTicketId() != null && isInGroup(Group.SUPPORTER);
    }

    private static final SprintfCommand sp = new SprintfCommand("Logged in as {0} via {1}.", Arrays.asList("${username", "${loginMethod"));

    private static final SprintfCommand inner = new SprintfCommand("{0} (on behalf of {1})", Arrays.asList("${user", "${target"));

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.println("<div>");
        vars.put("username", new Outputable() {

            @Override
            public void output(PrintWriter out, Language l, Map<String, Object> vars) {
                if (target != actor) {
                    vars.put("user", ((Organisation) target).getName().toString());
                    vars.put("target", actor.getPreferredName().toString());
                    inner.output(out, l, vars);
                } else {
                    out.println(actor.getPreferredName().toString());
                }
            }
        });
        sp.output(out, l, vars);
        out.println("</div>");
        if (supporterTicketId != null) {
            out.println("<div>");
            out.println(l.getTranslation("SupportTicket: "));
            out.println(HTMLEncoder.encodeHTML(supporterTicketId));
            out.println("</div>");
        }
    }

    public boolean canAssure() {
        return target instanceof User && ((User) target).canAssure();
    }
}
