package club.wpia.gigi.template;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;

import org.junit.Test;

import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.MailTemplate;
import club.wpia.gigi.testUtils.BusinessTest;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;

public class TestTemplateMail extends BusinessTest {

    private static final String TEST_MAIL = "test@mail.com";

    private void testExecuteMail(HashMap<String, Object> vars, String input) throws IOException {
        MailTemplate t = new MailTemplate(new StringReader(input));
        t.sendMail(Language.getInstance(Locale.ENGLISH), vars, TEST_MAIL);
    }

    HashMap<String, Object> vars = new HashMap<>();

    @Test
    public void testSimple() throws IOException {
        vars.put("var", "val");
        testExecuteMail(vars, "Subject: subj\n\n<?=$var?>l");
        TestMail tm = getMailReceiver().receive();
        assertEquals(MailTemplate.SUBJECT_TAG + "subj", tm.getSubject());
        assertThat(tm.getMessage(), startsWith("vall"));
    }

    @Test
    public void testVarNoEscape() throws IOException {
        vars.put("var", "val\">");
        vars.put("var2", "sl\">");
        testExecuteMail(vars, "Subject: a<?=$var?>b\n\n<?=$var2?>l");
        TestMail tm = getMailReceiver().receive();
        assertEquals(MailTemplate.SUBJECT_TAG + "aval\">b", tm.getSubject());
        assertThat(tm.getMessage(), startsWith("sl\">l"));

    }

    @Test
    public void testTranslate() throws IOException {
        testExecuteMail(vars, "Subject: a<?=_a<?>b\n\nc<?=_b\"?>l");
        TestMail tm = getMailReceiver().receive();
        assertEquals(MailTemplate.SUBJECT_TAG + "aa<b", tm.getSubject());
        assertThat(tm.getMessage(), startsWith("cb\"l"));

    }
}
