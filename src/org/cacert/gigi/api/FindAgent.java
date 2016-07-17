package org.cacert.gigi.api;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.pages.account.FindAgentAccess;
import org.cacert.gigi.util.ServerConstants;

public class FindAgent extends APIPoint {

    public static final String PATH_RESOLVE = "/find-agent/resolve";

    public static final String PATH_INFO = "/find-agent/info";

    public static final String PATH_MAIL = "/find-agent/email";

    public FindAgent() {}

    public static void register(HashMap<String, APIPoint> api) {
        APIPoint p = new FindAgent();
        api.put(PATH_RESOLVE, p);
        api.put(PATH_INFO, p);
        api.put(PATH_MAIL, p);
    }

    @Override
    public void process(HttpServletRequest req, HttpServletResponse resp, CertificateOwner u) throws IOException {
        if ( !(u instanceof Organisation)) {
            resp.sendError(500, "Error, invalid cert");
            return;
        }
        if ( !((Organisation) u).isSelfOrganisation()) {
            resp.sendError(500, "Error, invalid cert");
            return;
        }
        String pi = req.getPathInfo();
        if (pi.equals(PATH_RESOLVE)) {
            String serial = req.getParameter("serial");
            if (serial == null) {
                resp.sendError(500, "Error, requires serial");
                return;
            }
            Certificate c = Certificate.getBySerial(serial);
            if (c == null) {
                resp.sendError(500, "Error, requires serial");
                return;
            }
            CertificateOwner co = c.getOwner();
            if ( !(co instanceof User)) {
                resp.sendError(500, "Error, requires serial");
                return;
            }
            User us = (User) co;
            if ( !us.isInGroup(Group.LOCATE_AGENT)) {
                resp.setStatus(501);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().println("https://" + ServerConstants.getSecureHostNamePort() + FindAgentAccess.PATH);
                return;
            }
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().print(us.getId());
        } else if (pi.equals(PATH_INFO)) {
            resp.setContentType("text/plain; charset=UTF-8");
            String[] uids = req.getParameterValues("id");
            for (String i : uids) {
                User u1 = User.getById(Integer.parseInt(i));
                if ( !u1.isInGroup(Group.LOCATE_AGENT)) {
                    continue;
                }
                // date, recheck(?), name
                resp.getWriter().println(i + "," + u1.canAssure() + "," + u1.getName().toString());
            }
        } else if (pi.equals(PATH_MAIL)) {
            String id = req.getParameter("from");
            String rid = req.getParameter("to");
            String subject = req.getParameter("subject");
            String body = req.getParameter("body");
            if (id == null || rid == null || subject == null || body == null) {
                resp.sendError(500, "Error, parameter missing");
                return;
            }
            User from = User.getById(Integer.parseInt(id));
            User to = User.getById(Integer.parseInt(rid));
            if (from == null || to == null) {
                resp.sendError(500, "Error, user not found");
                return;
            }
            if ( !from.isInGroup(Group.LOCATE_AGENT) || !to.isInGroup(Group.LOCATE_AGENT)) {
                resp.sendError(501, "Error, user needs to enable access");
                return;

            }
            EmailProvider.getInstance().sendMail(to.getEmail(), "[Find Agent] " + subject, body, to.getEmail(), null, null, null, null, false);
        }
    }
}
