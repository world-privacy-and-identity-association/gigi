package club.wpia.gigi.api;

import java.io.IOException;
import java.math.BigInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.Certificate.RevocationType;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.dbObjects.User;

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
        if (tserial == null || tserial.isEmpty()) {
            resp.sendError(500, "Error, no Serial found");
            return;
        }

        Certificate c = Certificate.getBySerial(new BigInteger(tserial, 16));
        if (c == null || c.getOwner() != u) {
            resp.sendError(403, "Access Denied");
            return;
        }

        Job job = c.revoke(RevocationType.USER);
        job.waitFor(Job.WAIT_MIN);
        if (c.getStatus() != CertificateStatus.REVOKED) {
            resp.sendError(510, "Error, issuing timed out");
            return;
        }

        resp.getWriter().println("OK");

    }
}
