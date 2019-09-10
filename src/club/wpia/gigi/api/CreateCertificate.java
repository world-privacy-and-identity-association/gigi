package club.wpia.gigi.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.CertificateProfile;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.account.certs.CertificateRequest;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.CertExporter;

public class CreateCertificate extends APIPoint {

    public static final String PATH = "/account/certs/new";

    @Override
    public void process(HttpServletRequest req, HttpServletResponse resp, User u) throws IOException {
        String csr = req.getParameter("csr");
        if (csr == null) {
            resp.sendError(500, "Error, no CSR found");
            return;
        }
        CertificateProfile cp = null;
        String cpS = req.getParameter("profile");
        if (cpS != null) {
            cp = CertificateProfile.getByName(cpS);
            if (cp == null) {
                resp.sendError(500, "Error, profile not found");
                return;
            }
        }
        AuthorizationContext ctx = new AuthorizationContext(u, u, true);
        String asOrg = req.getParameter("asOrg");
        if (asOrg != null) {
            try {
                int i = Integer.parseInt(asOrg);
                Organisation o0 = null;
                for (Organisation o : u.getOrganisations()) {
                    if (o.getId() == i) {
                        o0 = o;
                        break;
                    }
                }
                if (o0 == null) {
                    resp.sendError(500, "Error, Organisation with id " + i + " not found.");
                    return;
                } else {
                    ctx = new AuthorizationContext(o0, u, true);
                }
            } catch (NumberFormatException e) {
                resp.sendError(500, "Error, as Org is not an integer");
                return;
            }
        }
        try {
            CertificateRequest cr = new CertificateRequest(ctx, csr, cp);
            Certificate result = cr.draft();
            Job job = result.issue(null, "2y", u);
            job.waitFor(60000);
            if (result.getStatus() != CertificateStatus.ISSUED) {
                resp.sendError(510, "Error, issuing timed out");
                return;
            }
            resp.addHeader("Content-Type", "text/plain");
            CertExporter.writeCertCrt(result, resp.getOutputStream(), req.getParameter("chain") != null, req.getParameter("noAnchor") == null, true);
            return;
        } catch (GeneralSecurityException e) {
            resp.sendError(500, "Crypto failed");
        } catch (GigiApiException e) {
            resp.setStatus(500);
            PrintWriter wr = resp.getWriter();
            e.formatPlain(wr);
        }
    }
}
