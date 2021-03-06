package club.wpia.gigi.api;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.LoginPage;

public abstract class APIPoint {

    public void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        X509Certificate cert = LoginPage.getCertificateFromRequest(req);
        if (cert == null) {
            resp.sendError(403, "Error, cert authing required. No cert found.");
            return;
        }
        BigInteger serial = LoginPage.extractSerialFormCert(cert);
        Certificate clientCert = Certificate.getBySerial(serial);
        CertificateOwner u = CertificateOwner.getByEnabledSerial(serial);
        if (u == null || clientCert == null) {
            resp.sendError(403, "Error, cert authing required. Serial not found: " + serial);
            return;
        }
        if (req.getMethod().equals("GET")) {
            if (u instanceof User) {
                processGet(req, resp, (User) u);
                return;
            } else {
                resp.sendError(500, "Error, requires a User certificate.");
                return;
            }
        }

        if ( !req.getMethod().equals("POST")) {
            resp.sendError(500, "Error, POST required.");
            return;
        }
        if (req.getQueryString() != null) {
            resp.sendError(500, "Error, no query String allowed.");
            return;
        }
        process(req, resp, u, clientCert);
    }

    protected void process(HttpServletRequest req, HttpServletResponse resp, CertificateOwner u, Certificate clientCert) throws IOException {
        process(req, resp, u);
    }

    protected void process(HttpServletRequest req, HttpServletResponse resp, CertificateOwner u) throws IOException {
        if (u instanceof User) {
            process(req, resp, (User) u);
        } else {
            resp.sendError(500, "Error, requires a User certificate.");
            return;
        }
    }

    protected void process(HttpServletRequest req, HttpServletResponse resp, User u) throws IOException {
        resp.sendError(500, "Error, Post not allowed.");
    }

    protected void processGet(HttpServletRequest req, HttpServletResponse resp, User u) throws IOException {
        resp.sendError(500, "Error, Get not allowed.");
    }
}
