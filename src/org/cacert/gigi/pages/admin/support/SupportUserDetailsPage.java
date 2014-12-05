package org.cacert.gigi.pages.admin.support;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.pages.Page;

public class SupportUserDetailsPage extends Page {

    public static final String PATH = "/support/user/";

    public SupportUserDetailsPage(String title) {
        super(title);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {}

}
