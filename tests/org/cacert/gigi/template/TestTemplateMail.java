package org.cacert.gigi.template;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.MailTemplate;
import org.cacert.gigi.testUtils.BusinessTest;
import org.cacert.gigi.testUtils.TestEmailReceiver.TestMail;
import org.junit.Test;

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
