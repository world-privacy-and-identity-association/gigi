package org.cacert.gigi.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;

public class CATSResolve extends APIPoint {

    public static final String PATH = "/cats/resolve";

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
        String target = req.getParameter("serial");
        if (target == null) {
            resp.sendError(500, "Error, requires a serial parameter");
            return;
        }

        CertificateOwner o = CertificateOwner.getByEnabledSerial(target.toLowerCase());
        if ( !(o instanceof User)) {
            resp.sendError(500, "Error, requires valid serial");
            return;
        }
        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().print(o.getId());
    }
}
