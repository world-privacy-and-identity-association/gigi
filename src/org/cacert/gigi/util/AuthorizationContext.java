package org.cacert.gigi.util;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Outputable;

public class AuthorizationContext implements Outputable {

    CertificateOwner target;

    User actor;

    String supporterTicketId;

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

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.println("<div>");
        out.println(l.getTranslation("Logged in as"));
        out.println(": ");

        if (target != actor) {
            out.println(((Organisation) target).getName() + " (" + actor.getName().toString() + ")");
        } else {
            out.println(actor.getName().toString());
        }

        out.println(l.getTranslation("with"));
        ((Outputable) vars.get("loginMethod")).output(out, l, vars);
        out.println();
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
