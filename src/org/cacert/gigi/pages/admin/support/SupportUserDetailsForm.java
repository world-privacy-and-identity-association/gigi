package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;

public class SupportUserDetailsForm extends Form {

    public SupportUserDetailsForm(HttpServletRequest hsr) {
        super(hsr);
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        return false;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {}

}
