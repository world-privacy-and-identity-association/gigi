package org.cacert.gigi.pages.account;

import static org.cacert.gigi.Gigi.USER;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.User;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.HTMLEncoder;

public class MyDetails extends Page {

    public MyDetails() {
        super("My Details");
    }

    public static final String PATH = "/account/details";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = (User) req.getSession().getAttribute(USER);

        PrintWriter out = resp.getWriter();
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("fname", HTMLEncoder.encodeHTML(u.getFname()));
        map.put("mname", u.getMname() == null ? "" : HTMLEncoder.encodeHTML(u.getMname()));
        map.put("lname", HTMLEncoder.encodeHTML(u.getLname()));
        map.put("suffix", u.getSuffix() == null ? "" : HTMLEncoder.encodeHTML(u.getSuffix()));
        DateSelector ds = new DateSelector("day", "month", "year");
        map.put("DoB", ds);
        map.put("details", "");
        getDefaultTemplate().output(out, getLanguage(req), map);

    }
}
