package org.cacert.gigi.pages.main;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.cacert.gigi.pages.Page;

public class RegisterPage extends Page {

	public static final String PATH = "/register";

	public RegisterPage() {
		super("Register");
	}

	@Override
	public void doGet(ServletRequest req, ServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		out.print("<p>");
		out.print(translate(
				req,
				"By joining CAcert and becoming a member, you agree to the CAcert Community Agreement. Please take a moment now to read that and agree to it; this will be required to complete the process of joining."));
		out.println("</p>");
		out.print("<p>");
		out.print(translate(
				req,
				"Warning! This site requires cookies to be enabled to ensure your privacy and security. This site uses session cookies to store temporary values to prevent people from copying and pasting the session ID to someone else exposing their account, personal details and identity theft as a result."));
		out.println("</p>");
		out.print("<p style=\"border:dotted 1px #900;padding:0.3em;background-color:#ffe;\"><b>");
		out.print(translate(
				req,
				"Note: Please enter your date of birth and names as they are written in your official documents."));
		out.println("</b><br /><br/>");
		out.println(translate(
				req,
				"Because CAcert is a certificate authority (CA) people rely on us knowing about the identity of the users of our certificates. So even as we value privacy very much, we need to collect at least some basic information about our members. This is especially the case for everybody who wants to take part in our web of trust."));
		out.print(translate(
				req,
				"Your private information will be used for internal procedures only and will not be shared with third parties."));
		out.println("</p>");
		out.print("<p style=\"border:dotted 1px #900;padding:0.3em;background-color:#ffe;\">");
		out.println(translate(
				req,
				"A proper password wouldn't match your name or email at all, it contains at least 1 lower case letter, 1 upper case letter, a number, white space and a misc symbol. You get additional security for being over 15 characters and a second additional point for having it over 30. The system starts reducing security if you include any section of your name, or password or email address or if it matches a word from the english dictionary..."));
		out.println("<br/><br/>");
		out.print("<b>");
		out.print(translate(req,
				"Note: White spaces at the beginning and end of a password will be removed."));
		out.println("</b>");
		out.println("</p>");

	}
}
