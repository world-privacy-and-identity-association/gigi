package org.cacert.gigi.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.account.certs.CertificateRequest;
import org.cacert.gigi.util.Job;
import org.cacert.gigi.util.PEM;

public class GigiAPI extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pi = req.getPathInfo();
        if (pi == null) {
            return;
        }
        if (pi.equals("/security/csp/report")) {
            ServletInputStream sis = req.getInputStream();
            InputStreamReader isr = new InputStreamReader(sis, "UTF-8");
            StringBuffer strB = new StringBuffer();
            char[] buffer = new char[4 * 1024];
            int len;
            while ((len = isr.read(buffer)) > 0) {
                strB.append(buffer, 0, len);
            }
            System.out.println(strB);
            return;
        }
        X509Certificate cert = LoginPage.getCertificateFromRequest(req);
        if (cert == null) {
            resp.sendError(403, "Error, cert authing required.");
            return;
        }
        String serial = LoginPage.extractSerialFormCert(cert);
        User u = LoginPage.fetchUserBySerial(serial);

        if (pi.equals("/account/certs/new")) {

            if ( !req.getMethod().equals("POST")) {
                resp.sendError(500, "Error, POST required.");
                return;
            }
            if (req.getQueryString() != null) {
                resp.sendError(500, "Error, no query String allowed.");
                return;
            }
            String csr = req.getParameter("csr");
            if (csr == null) {
                resp.sendError(500, "Error, no CSR found");
                return;
            }
            try {
                CertificateRequest cr = new CertificateRequest(u, csr);
                Certificate result = cr.draft();
                Job job = result.issue(null, "2y");
                job.waitFor(60000);
                if (result.getStatus() != CertificateStatus.ISSUED) {
                    resp.sendError(510, "Error, issuing timed out");
                    return;
                }
                resp.getWriter().println(PEM.encode("CERTIFICATE", result.cert().getEncoded()));
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (GigiApiException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
