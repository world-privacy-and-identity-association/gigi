package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.CountryCode;
import org.cacert.gigi.dbObjects.CountryCode.CountryCodeType;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.Template;

public class CountrySelector implements Outputable {

    private static final Template t = new Template(CountrySelector.class.getResource("CountrySelector.templ"));

    private CountryCode[] all = CountryCode.getCountryCodes(CountryCodeType.CODE_2_CHARS);

    private String name;

    private CountryCode selected;

    private boolean optional;

    public CountrySelector(String name, boolean optional) {
        this.name = name;
        this.optional = optional;
    }

    public CountrySelector(String name, boolean optional, CountryCode state) {
        this(name, optional);
        selected = state == null ? null : state.convertToCountryCodeType(CountryCodeType.CODE_2_CHARS);
    }

    public void update(HttpServletRequest r) throws GigiApiException {
        String vS = r.getParameter(name);

        selected = null;

        if (vS == null || vS.equals("invalid")) {
            if (optional) {
                return;
            } else {
                throw new GigiApiException("Country code required.");
            }
        }

        selected = CountryCode.getCountryCode(vS, CountryCodeType.CODE_2_CHARS);
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("countryCode", new ArrayIterable<CountryCode>(all) {

            @Override
            public void apply(CountryCode t, Language l, Map<String, Object> vars) {
                vars.put("cc", t.getCountryCode());
                vars.put("display", t.getCountry());
                if (selected != null && t.getCountryCode().equals(selected.getCountryCode())) {
                    vars.put("selected", "selected");
                } else {
                    vars.put("selected", "");
                }
            }

        });

        vars.put("optional", optional);
        vars.put("name", name);

        t.output(out, l, vars);
    }

    public CountryCode getCountry() {
        return selected;
    }

}
