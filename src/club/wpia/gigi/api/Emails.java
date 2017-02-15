package club.wpia.gigi.api;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.EmailAddress;
import club.wpia.gigi.dbObjects.User;

public class Emails extends APIPoint {

    public static final String PATH = "/account/emails";

    @Override
    public void processGet(HttpServletRequest req, HttpServletResponse resp, User u) throws IOException {
        EmailAddress[] mails = u.getEmails();
        resp.setContentType("application/json; charset=UTF-8");
        JSONWriter jw = new JSONWriter(resp.getWriter());
        jw.array();
        for (EmailAddress emailAddress : mails) {
            Date p = emailAddress.getLastPing(true);
            jw.object();
            jw.key("id");
            jw.value(emailAddress.getId());
            jw.key("lastPing");
            jw.value((p == null ? 0 : p.getTime()));
            jw.key("address");
            jw.value(emailAddress.getAddress());
            jw.endObject();
        }
        jw.endArray();
    }

    @Override
    protected void process(HttpServletRequest req, HttpServletResponse resp, User u) throws IOException {
        try {
            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(500, "No parameter 'email'.");
                return;
            }
            new EmailAddress(u, email, u.getPreferredLocale());
        } catch (IllegalArgumentException e) {
            resp.sendError(500, "Invalid email");
        } catch (GigiApiException e) {
            resp.setStatus(500);
            resp.setContentType("text/plain; charset=UTF-8");
            e.formatPlain(resp.getWriter());
        }
    }

}
