package club.wpia.gigi.output;

import java.io.PrintWriter;

import club.wpia.gigi.localisation.Language;

public class SimpleMenuItem extends SimpleUntranslatedMenuItem {

    public SimpleMenuItem(String href, String name) {
        super(href, name);
    }

    @Override
    protected void printContent(PrintWriter out, Language l) {
        out.print(l.getTranslation(name));
    }

}
