package club.wpia.gigi.ocsp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;

import club.wpia.gigi.crypto.OCSPRequest;
import club.wpia.gigi.crypto.OCSPResponse;
import club.wpia.gigi.database.DatabaseConnection;
import club.wpia.gigi.database.DatabaseConnection.Link;
import sun.security.provider.certpath.CertId;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

/**
 * This is the entry point for OCSP Issuing
 */
public class OCSPResponder extends HttpServlet {

    static final Logger log = Logger.getLogger(OCSPResponder.class.getName());

    private static final long serialVersionUID = 1L;

    private final OCSPIssuerManager mgm = new OCSPIssuerManager();

    public OCSPResponder() {}

    public static byte[] calcKeyHash(X509Certificate x, MessageDigest md) {
        try {
            DerInputStream dis = new DerInputStream(x.getPublicKey().getEncoded());
            DerValue[] seq = dis.getSequence(2);
            byte[] bitString = seq[1].getBitString();
            return md.digest(bitString);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        new Thread(mgm).start();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (Link l = DatabaseConnection.newLink(true)) {
            byte[] bytes;
            if (req.getMethod().equals(HttpMethod.POST.toString())) {
                bytes = getBytes(req);
                if (bytes == null) {
                    resp.sendError(500);
                    resp.getWriter().println("OCSP request too large");
                    return;
                }
            } else {
                bytes = Base64.getDecoder().decode(req.getPathInfo().substring(1));
            }
            OCSPRequest or = new OCSPRequest(bytes);
            byte[] res = respond(or);
            resp.setContentType("application/ocsp-response");
            resp.getOutputStream().write(res);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    private byte[] respond(OCSPRequest or) throws GeneralSecurityException, IOException {
        List<CertId> ids = or.getCertIds();
        if (ids.size() != 1) {
            // We don't implement multi-requests as:
            // a) we don't know of applications using them
            // b) this will introduce additional complexity
            // c) there is at least one corner-case that needs to be thought of:
            // an OCSP request might contain requests for certs from different
            // issuers, what issuer's ocsp cert should sign the response?
            return OCSPResponse.invalid();
        }
        CertId id = ids.get(0);
        OCSPIssuerId iid = new OCSPIssuerId(id);
        Map<OCSPIssuerId, OCSPIssuer> m0;
        m0 = mgm.get(iid.getAlg());
        if (m0 == null) {
            log.warning("Algorithm " + iid.getAlg() + " not indexed.");
            return OCSPResponse.invalid();
        }
        OCSPIssuer iss = m0.get(iid);

        if (iss == null) {
            log.warning("CertID not handled:\n" +//
                    Base64.getEncoder().encodeToString(id.getIssuerNameHash()) + "\n"//
                    + Base64.getEncoder().encodeToString(id.getIssuerKeyHash()) + "\n" //
                    + id.getHashAlgorithm() + "\n"//
                    + id.getSerialNumber().toString(16));

            return OCSPResponse.invalid();
        }
        return iss.respondBytes(or, id);
    }

    private byte[] getBytes(HttpServletRequest req) throws IOException {
        InputStream i = req.getInputStream();
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = i.read(buf)) > 0) {
            o.write(buf, 0, len);
            if (o.size() > 64 * 1024) {
                // for now have 64k as maximum
                return null;
            }
        }
        byte[] dat = o.toByteArray();
        return dat;
    }

}
