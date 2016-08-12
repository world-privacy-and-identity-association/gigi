package org.cacert.gigi.output.template;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.cacert.gigi.email.SendMail;
import org.cacert.gigi.localisation.Language;

public class MailTemplate extends Template {

    private static final Template FOOTER = new Template(MailTemplate.class.getResource("MailFooter.templ"));

    public static final String SUBJECT_TAG = "[SomeCA] ";

    private TemplateBlock subjectBlock;

    public MailTemplate(URL u) {
        super(u);
    }

    public MailTemplate(Reader r) {
        super(r);
    }

    @Override
    protected ParseResult parse(Reader r) throws IOException {
        StringBuilder strb = new StringBuilder();
        int ct = 0;
        int c;
        while ((c = r.read()) > 0) {
            if (c == '\n') {
                ct++;
                if (ct == 2) {
                    break;
                }
            } else {
                ct = 0;
            }
            strb.append((char) c);
        }
        String[] lines = strb.toString().split("\n");
        for (int i = 0; i < lines.length; i++) {
            String[] lineParts = lines[i].split(": ", 2);
            if (lineParts.length != 2) {
                throw new IOException("Mail template header malformed.");
            }
            if (lineParts[0].equals("Subject")) {
                subjectBlock = parseContent(new StringReader(lineParts[1])).getBlock(null);
            }
        }
        if (subjectBlock == null) {
            throw new IOException("Mail template without subject line.");
        }
        return parseContent(r);
    }

    public void sendMail(Language l, Map<String, Object> vars, String to) throws IOException {
        tryReload();
        vars.put(Outputable.OUT_KEY_PLAIN, true);

        String body = runTemplate(this, l, vars);
        body += runTemplate(FOOTER, l, vars);
        String subject = runTemplate(subjectBlock, l, vars);

        SendMail.getInstance().sendMail(to, SUBJECT_TAG + subject, body, null, null, null, null, false);
    }

    private static String runTemplate(Outputable toRun, Language l, Map<String, Object> vars) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        toRun.output(pw, l, vars);
        pw.close();
        return sw.toString();
    }

    @Override
    public void addTranslations(Collection<String> s) {
        subjectBlock.addTranslations(s);
        super.addTranslations(s);
    }

}
