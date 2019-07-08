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

    private final CertificateOwner target;

    private final User actor;

    private final String supporterTicketId;

    private final boolean isStronglyAuthenticated;

    public AuthorizationContext(CertificateOwner target, User actor, boolean isStronglyAuthenticated) {
        if (actor == null) {
            throw new Error("Internal Error: The actor of an AuthorizationContext must not be null!");
        }
        if (target == null) {
            throw new Error("Internal Error: The target of an AuthorizationContext must not be null!");
        }
        this.target = target;
        this.actor = actor;
        this.supporterTicketId = null;
        this.isStronglyAuthenticated = isStronglyAuthenticated;
    }

    public AuthorizationContext(User actor, String supporterTicket) throws GigiApiException {
        if (actor == null) {
            throw new Error("Internal Error: The actor of an AuthorizationContext must not be null!");
        }
        if (supporterTicket == null) {
            throw new Error("Internal Error: The AuthorizationContext for a Support Engineer requires a valid ticket!");
        }
        this.target = actor;
        this.actor = actor;
        if ( !isInGroup(Group.SUPPORTER)) {
            throw new GigiApiException("requires a supporter");
        }
        this.supporterTicketId = supporterTicket;
        this.isStronglyAuthenticated = true;
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

    public static User getActor(AuthorizationContext ac) {
        if (ac == null) {
            return null;
        }
        return ac.getActor();
    }

    public String getSupporterTicketId() {
        return supporterTicketId;
    }

    public boolean canSupport() {
        return getSupporterTicketId() != null && isInGroup(Group.SUPPORTER) && isStronglyAuthenticated();
    }

    private static final SprintfCommand sp = new SprintfCommand("Logged in as {0} via {1}.", Arrays.asList("${username", "${loginMethod"));

    private static final SprintfCommand inner = new SprintfCommand("{0}, acting as {1},", Arrays.asList("${user", "${target"));

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.println("<div>");
        vars.put("username", new Outputable() {

            @Override
            public void output(PrintWriter out, Language l, Map<String, Object> vars) {
                if (target != actor) {
                    vars.put("target", ((Organisation) target).getName().toString());
                    vars.put("user", actor.getPreferredName().toString());
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

    public boolean canVerify() {
        return target instanceof User && ((User) target).canVerify();
    }

    public boolean isStronglyAuthenticated() {
        return isStronglyAuthenticated;
    }
}
