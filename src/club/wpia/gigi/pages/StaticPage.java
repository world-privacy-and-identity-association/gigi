package club.wpia.gigi.pages;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.output.template.Template;

public class StaticPage extends Page {

    private Template content;

    public StaticPage(String title, InputStream content) throws UnsupportedEncodingException {
        super(title);
        this.content = new Template(new InputStreamReader(content, "UTF-8"));
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        content.output(resp.getWriter(), getLanguage(req), getDefaultVars(req));
    }

}
