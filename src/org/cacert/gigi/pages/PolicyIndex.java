package org.cacert.gigi.pages;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PolicyIndex extends Page {

    public PolicyIndex() {
        super("SomeCA.org Policies");
    }

    File root = new File("static/www/policy");

    public static final String DEFAULT_PATH = "/policy";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        out.println("<ul>");
        File[] files = root.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if ( !name.endsWith(".html")) {
                    continue;
                }
                String display = name.replaceFirst("\\.html$", "");

                out.print("<li><a href='");
                out.print(name);
                out.print("'>");
                out.print(display);
                out.println("</a></li>");
            }
        }
        out.println("</ul>");
    }

    @Override
    public boolean needsLogin() {
        return false;
    }

}
