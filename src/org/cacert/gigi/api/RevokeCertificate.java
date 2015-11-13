package org.cacert.gigi.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.dbObjects.Job;
import org.cacert.gigi.dbObjects.User;

public class RevokeCertificate extends APIPoint {

    public static final String PATH = "/account/certs/revoke";

    @Override
    public void process(HttpServletRequest req, HttpServletResponse resp, User u) throws IOException {

        if ( !req.getMethod().equals("POST")) {
            resp.sendError(500, "Error, POST required.");
            return;
        }
        if (req.getQueryString() != null) {
            resp.sendError(500, "Error, no query String allowed.");
            return;
        }
        String tserial = req.getParameter("serial");
        if (tserial == null) {
            resp.sendError(500, "Error, no Serial found");
            return;
        }
        try {
            Certificate c = Certificate.getBySerial(tserial);
            if (c == null || c.getOwner() != u) {
                resp.sendError(403, "Access Denied");
                return;
            }
            Job job = c.revoke();
            job.waitFor(60000);
            if (c.getStatus() != CertificateStatus.REVOKED) {
                resp.sendError(510, "Error, issuing timed out");
                return;
            }
            resp.getWriter().println("OK");
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
