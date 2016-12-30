package org.cacert.gigi.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.User;

public class CATSResolve extends CATSRestrictedApi {

    public static final String PATH = "/cats/resolve";

    @Override
    public void processAuthenticated(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String target = req.getParameter("serial");
        if (target == null) {
            resp.sendError(500, "Error, requires a serial parameter");
            return;
        }
        target = target.toLowerCase();
        Certificate clientCert = Certificate.getBySerial(target);
        if (clientCert == null) {
            resp.sendError(500, "Error, requires valid serial");
            return;
        }
        CertificateOwner o = CertificateOwner.getByEnabledSerial(target);
        if ( !(o instanceof User)) {
            resp.sendError(500, "Error, requires valid serial");
            return;
        }
        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().print(o.getId());
    }
}
