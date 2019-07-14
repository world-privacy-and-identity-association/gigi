package club.wpia.gigi.pages.orga;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.pages.ManagedFormPage;
import club.wpia.gigi.util.AuthorizationContext;

public class SwitchOrganisation extends ManagedFormPage {

    public static final String PATH = "/orga/switch-orga";

    public SwitchOrganisation() {
        super("Switch to Organisation", MyOrganisationsForm.class);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.getActor().getOrganisations().size() != 0 && ac.isStronglyAuthenticated();
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new MyOrganisationsForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }
}
