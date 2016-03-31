package org.cacert.gigi.api;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Job;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.account.certs.CertificateRequest;
import org.cacert.gigi.util.AuthorizationContext;
import org.cacert.gigi.util.PEM;

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
                resp.sendError(500, "Error, profile " + cpS + "not found");
                return;
            }
        }
        try {
            CertificateRequest cr = new CertificateRequest(new AuthorizationContext(u, u), csr, cp);
            Certificate result = cr.draft();
            Job job = result.issue(null, "2y", u);
            job.waitFor(60000);
            if (result.getStatus() != CertificateStatus.ISSUED) {
                resp.sendError(510, "Error, issuing timed out");
                return;
            }
            resp.getWriter().println(PEM.encode("CERTIFICATE", result.cert().getEncoded()));
            return;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (GigiApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
