package org.cacert.gigi.pages.wot;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.Template;
import org.cacert.gigi.pages.Page;

public class AssurePage extends Page {
	public static final String PATH = "/wot/assure";
	DateSelector ds = new DateSelector("day", "month", "year");
	Template t;

	public AssurePage() {
		super("Assure someone");
		t = new Template(new InputStreamReader(
				AssurePage.class.getResourceAsStream("AssureeSearch.templ")));
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		HashMap<String, Object> vars = new HashMap<String, Object>();
		vars.put("DoB", ds);
		t.output(out, getLanguage(req), vars);
	}
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		System.out.println("searching for");
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
					"SELECT id FROM users WHERE email=? AND dob=?");
			ps.setString(1, req.getParameter("email"));
			String day = req.getParameter("year") + "-"
					+ req.getParameter("month") + "-" + req.getParameter("day");
			ps.setString(2, day);
			ResultSet rs = ps.executeQuery();
			int id = 0;
			if (rs.next()) {
				id = rs.getInt(1);
			}
			if (rs.next()) {
				out.println("Error, ambigous user. Please contact support@cacert.org");
			} else {
				out.println("Found member: " + id);
			}

			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
