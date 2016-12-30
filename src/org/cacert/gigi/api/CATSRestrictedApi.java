package org.cacert.gigi.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.SANType;
import org.cacert.gigi.dbObjects.Certificate.SubjectAlternateName;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.util.ServerConstants;

public abstract class CATSRestrictedApi extends APIPoint {

    @Override
    public final void process(HttpServletRequest req, HttpServletResponse resp, CertificateOwner u, Certificate clientCert) throws IOException {
        if ( !(u instanceof Organisation)) {
            resp.sendError(500, "Error, invalid cert");
            return;
        }
        if ( !((Organisation) u).isSelfOrganisation()) {
            resp.sendError(500, "Error, invalid cert");
            return;
        }
        if ( !hasMail(clientCert, ServerConstants.getQuizMailAddress())) {
            resp.sendError(500, "Error, invalid cert");
            return;
        }
        processAuthenticated(req, resp);
    }

    public abstract void processAuthenticated(HttpServletRequest req, HttpServletResponse resp) throws IOException;

    public boolean hasMail(Certificate clientCert, String mail) {
        for (SubjectAlternateName a : clientCert.getSANs()) {
            if (a.getType() == SANType.EMAIL && a.getName().equals(mail)) {
                return true;
            }
        }
        return false;
    }
}
