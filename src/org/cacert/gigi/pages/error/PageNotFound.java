package org.cacert.gigi.pages.error;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.pages.Page;

public class PageNotFound extends Page {

    public static final String MESSAGE_ATTRIBUTE = "message-Str";

    public PageNotFound() {
        super("File not found!");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HashMap<String, Object> vars = new HashMap<>();
        Object customMessage = req.getAttribute(MESSAGE_ATTRIBUTE);
        if (customMessage == null) {
            customMessage = getLanguage(req).getTranslation("Due to recent site changes bookmarks may no longer be valid, please update your bookmarks.");
        }
        vars.put("message", customMessage);
        getDefaultTemplate().output(resp.getWriter(), Page.getLanguage(req), vars);
    }

    @Override
    public boolean needsLogin() {
        return false;
    }

}
