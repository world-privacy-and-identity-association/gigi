package club.wpia.gigi.output;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Country.CountryCodeType;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.output.template.Template;

public class CountrySelector implements Outputable {

    private static final Template t = new Template(CountrySelector.class.getResource("CountrySelector.templ"));

    private List<Country> all = Country.getCountries();

    private String name;

    private Country selected;

    private boolean optional;

    public CountrySelector(String name, boolean optional) {
        this.name = name;
        this.optional = optional;
    }

    public CountrySelector(String name, boolean optional, Country country) {
        this(name, optional);
        selected = country;
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

        selected = Country.getCountryByCode(vS, CountryCodeType.CODE_2_CHARS);
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("countryCode", new IterableIterable<Country>(all) {

            @Override
            public void apply(Country t, Language l, Map<String, Object> vars) {
                vars.put("cc", t.getCode());
                vars.put("display", t.getName());
                if (selected != null && t.getCode().equals(selected.getCode())) {
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

    public Country getCountry() {
        return selected;
    }

}
