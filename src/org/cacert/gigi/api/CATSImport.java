package org.cacert.gigi.api;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.CATS;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;

public class CATSImport extends APIPoint {

    public static final String PATH = "/cats_import";

    @Override
    public void process(HttpServletRequest req, HttpServletResponse resp, CertificateOwner u) throws IOException {
        if ( !(u instanceof Organisation)) {
            resp.sendError(500, "Error, invalid cert");
            return;
        }
        if ( !"CAcert".equals(((Organisation) u).getName())) {
            resp.sendError(500, "Error, invalid cert");
            return;

        }
        String target = req.getParameter("serial");
        String testType = req.getParameter("variant");
        String date = req.getParameter("date");
        if (target == null || testType == null || date == null) {
            resp.sendError(500, "Error, requires serial, variant and date");
            return;
        }
        // TODO is "byEnabledSerial" desired?
        CertificateOwner o = CertificateOwner.getByEnabledSerial(target);
        if ( !(o instanceof User)) {
            resp.sendError(500, "Error, requires valid serial");
            return;
        }
        System.out.println("CATS: " + target + ": " + testType);
        User targetUser = (User) o;
        System.out.println(targetUser.getId());
        CATS.enterResult(targetUser, testType, new Date(Long.parseLong(date)));
    }
}
