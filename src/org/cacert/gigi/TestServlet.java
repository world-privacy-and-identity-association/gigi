package org.cacert.gigi;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.SSLEngine;
import javax.security.auth.x500.X500Principal;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;

public class TestServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Request r = (Request) req;
		HttpChannel<?> hc = r.getHttpChannel();
		EndPoint ep = hc.getEndPoint();
		SSLEngine se;
		Enumeration<String> names = req.getAttributeNames();
		X509Certificate[] cert = (X509Certificate[]) req
				.getAttribute("javax.servlet.request.X509Certificate");
		int keySize = (Integer) req
				.getAttribute("javax.servlet.request.key_size");
		String ciphers = (String) req
				.getAttribute("javax.servlet.request.cipher_suite");
		String sid = (String) req
				.getAttribute("javax.servlet.request.ssl_session_id");
		PrintWriter out = resp.getWriter();
		out.println("KeySize: " + keySize);
		out.println("cipher: " + ciphers);
		X509Certificate c1 = cert[0];
		out.println("Serial:" + c1.getSerialNumber());
		X500Principal client = c1.getSubjectX500Principal();
		out.println("Name " + client.getName());
		out.println(client.getName(X500Principal.RFC1779));
		out.println(client.getName(X500Principal.RFC2253));
		out.println("signature: " + c1.getSigAlgName());
		out.println("issuer: " + c1.getSubjectX500Principal());
		out.println("certCount: " + cert.length);
	}
}
