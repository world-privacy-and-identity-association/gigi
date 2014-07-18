package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.cacert.gigi.Certificate;
import org.cacert.gigi.Digest;
import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.HashAlgorithms;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;

import sun.security.pkcs10.PKCS10;

/**
 * This class represents a form that is used for issuing certificates.
 * 
 * This class uses "sun.security" and therefore needs "-XDignore.symbol.file"
 * 
 */
public class IssueCertificateForm extends Form {
	User u;
	Digest selectedDigest = Digest.getDefault();
	boolean login;
	String csr;

	private final static Template t = new Template(IssueCertificateForm.class.getResource("IssueCertificateForm.templ"));

	public IssueCertificateForm(HttpServletRequest hsr) {
		super(hsr);
		u = LoginPage.getUser(hsr);
	}

	Certificate result;

	public Certificate getResult() {
		return result;
	}

	@Override
	public boolean submit(PrintWriter out, HttpServletRequest req) {
		String csr = req.getParameter("CSR");
		String spkac = req.getParameter("spkac");
		try {
			if (csr != null) {
				PKCS10 parsed = parseCSR(csr);
				out.println(parsed.getSubjectName().getCommonName());
				out.println(parsed.getSubjectName().getCountry());
				out.println("CSR DN: " + parsed.getSubjectName() + "<br/>");
				PublicKey pk = parsed.getSubjectPublicKeyInfo();
				out.println("Type: " + pk.getAlgorithm() + "<br/>");
				if (pk instanceof RSAPublicKey) {
					out.println("Exponent: " + ((RSAPublicKey) pk).getPublicExponent() + "<br/>");
					out.println("Length: " + ((RSAPublicKey) pk).getModulus().bitLength());
				} else if (pk instanceof DSAPublicKey) {
					DSAPublicKey dpk = (DSAPublicKey) pk;
					out.println("Length: " + dpk.getY().bitLength() + "<br/>");
					out.println(dpk.getParams());
				} else if (pk instanceof ECPublicKey) {
					ECPublicKey epk = (ECPublicKey) pk;
					out.println("Length-x: " + epk.getW().getAffineX().bitLength() + "<br/>");
					out.println("Length-y: " + epk.getW().getAffineY().bitLength() + "<br/>");
					out.println(epk.getParams().getCurve());
				}
				out.println("<br/>digest: sha256<br/>");
				this.csr = csr;
			} else if (spkac != null) {

			} else {
				login = "1".equals(req.getParameter("login"));
				String hashAlg = req.getParameter("hash_alg");
				if (hashAlg != null) {
					selectedDigest = Digest.valueOf(hashAlg);
				}
				if (req.getParameter("CCA") == null) {
					outputError(out, req, "You need to accept the CCA.");
					return false;
				}
				System.out.println("issuing " + selectedDigest);
				result = new Certificate(LoginPage.getUser(req).getId(), "/commonName=CAcert WoT User",
					selectedDigest.toString(), this.csr);
				try {
					result.issue().waitFor(60000);
					return true;
				} catch (SQLException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
		return false;
	}

	private PKCS10 parseCSR(String csr) throws IOException, GeneralSecurityException {
		csr = csr.replaceFirst("-----BEGIN (NEW )?CERTIFICATE REQUEST-----", "");
		csr = csr.replaceFirst("-----END (NEW )?CERTIFICATE REQUEST-----", "");
		csr = csr.replace("\r", "");
		csr = csr.replace("\n", "");
		byte[] b = Base64.getDecoder().decode(csr);
		// Also checks signature validity
		return new PKCS10(b);
	}

	@Override
	protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
		HashMap<String, Object> vars2 = new HashMap<String, Object>(vars);
		vars2.put("CCA", "<a href='/policy/CAcertCommunityAgreement.html'>CCA</a>");

		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
				"SELECT `id`,`email` from `email` WHERE `memid`=? AND `deleted`=0");
			ps.setInt(1, u.getId());
			final ResultSet rs = ps.executeQuery();
			vars2.put("emails", new IterableDataset() {

				@Override
				public boolean next(Language l, Map<String, Object> vars) {
					try {
						if (!rs.next()) {
							return false;
						}
						vars.put("id", rs.getString(1));
						vars.put("value", rs.getString(2));
						return true;
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return false;
				}
			});
			vars2.put("hashs", new HashAlgorithms(selectedDigest));
			t.output(out, l, vars2);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
