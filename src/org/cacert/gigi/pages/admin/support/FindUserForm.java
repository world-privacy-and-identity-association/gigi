package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;

public class FindUserForm extends Form {

    private User users[];

    private static Template t;
    static {
        t = new Template(FindDomainForm.class.getResource("FindUserForm.templ"));
    }

    public FindUserForm(HttpServletRequest hsr) {
        super(hsr);
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        User[] users = User.findByEmail(req.getParameter("email"));
        if (users.length == 0) {
            throw (new GigiApiException("No users found matching " + req.getParameter("email")));
        }
        this.users = users;
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

    public User[] getUsers() {
        return users;
    }

}
