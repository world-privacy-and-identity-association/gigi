package org.cacert.gigi.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;

public class EmailReping extends APIPoint {

    public static final String PATH = "/account/emails/reping";

    @Override
    protected void process(HttpServletRequest req, HttpServletResponse resp, User u) throws IOException {
        try {
            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(500, "No parameter 'email'.");
                return;
            }
            for (EmailAddress e : u.getEmails()) {
                if (e.getAddress().equals(email)) {
                    e.requestReping(Language.getInstance(u.getPreferredLocale()));
                    resp.setContentType("text/plain; charset=UTF-8");
                    return;
                }
            }
            resp.sendError(500, "Error, Email address not found.");
        } catch (IllegalArgumentException e) {
            resp.sendError(500, "Invalid email");
        } catch (GigiApiException e) {
            resp.setStatus(500);
            resp.setContentType("text/plain; charset=UTF-8");
            e.formatPlain(resp.getWriter());
        }
    }
}
