package club.wpia.gigi.output.template;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.DateSelector;
import club.wpia.gigi.util.DayDate;
import club.wpia.gigi.util.EditDistance;
import club.wpia.gigi.util.HTMLEncoder;

/**
 * Represents a loaded template file.
 */
public class Template implements Outputable {

    protected static class ParseResult {

        TemplateBlock block;

        String endType;

        public ParseResult(TemplateBlock block, String endType) {
            this.block = block;
            this.endType = endType;
        }

        public String getEndType() {
            return endType;
        }

        public TemplateBlock getBlock(String reqType) {
            if (endType == null && reqType == null) {
                return block;
            }
            if (endType == null || reqType == null) {
                throw new Error("Invalid block type: " + endType);
            }
            if (endType.equals(reqType)) {
                return block;
            }
            throw new Error("Invalid block type: " + endType);
        }
    }

    private TemplateBlock data;

    private long lastLoaded;

    private File source;

    private static final Pattern CONTROL_PATTERN = Pattern.compile(" ?([a-zA-Z]+)\\(\\$([^)]+)\\) ?\\{ ?");

    private static final Pattern ELSE_PATTERN = Pattern.compile(" ?\\} ?else ?\\{ ?");

    private static final String[] POSSIBLE_CONTROL_PATTERNS = new String[] {
            "if", "else", "foreach"
    };

    private static final String UNKOWN_CONTROL_STRUCTURE_MSG = "Unknown control structure \"%s\", did you mean \"%s\"?";

    /**
     * Creates a new template by parsing the contents from the given URL. This
     * constructor will fail on syntax error. When the URL points to a file,
     * {@link File#lastModified()} is monitored for changes of the template.
     * 
     * @param u
     *            the URL to load the template from. UTF-8 is chosen as charset.
     */
    public Template(URL u) {
        try (Reader r = new InputStreamReader(u.openStream(), "UTF-8")) {
            try {
                if (u.getProtocol().equals("file")) {
                    source = new File(u.toURI());
                    lastLoaded = source.lastModified() + 1000;
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            data = parse(r).getBlock(null);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * Creates a new template by parsing the contents from the given reader.
     * This constructor will fail on syntax error.
     * 
     * @param r
     *            the Reader containing the data.
     */
    public Template(Reader r) {
        try {
            data = parse(r).getBlock(null);
            r.close();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    protected ParseResult parse(Reader r) throws IOException {
        return parseContent(r);
    }

    protected ParseResult parseContent(Reader r) throws IOException {
        ParseContext context = new ParseContext(r);
        ParseResult result = parseContent(context);
        if (context.parseException.isEmpty()) {
            return result;
        }
        while (context.curChar != -1) {
            parseContent(context);
        }
        throw context.parseException;
    }

    protected ParseResult parseContent(ParseContext context) throws IOException {
        LinkedList<String> splitted = new LinkedList<String>();
        LinkedList<Translatable> commands = new LinkedList<Translatable>();
        StringBuffer buf = new StringBuffer();
        String blockType = null;
        ParseContext tContext = null;
        outer:
        while (true) {
            if (tContext != null) {
                context.merge(tContext);
            }
            while ( !endsWith(buf, "<?")) {
                int ch = context.read();
                if (ch == -1) {
                    break outer;
                }
                buf.append((char) ch);
                if (endsWith(buf, "\\\n")) {
                    buf.delete(buf.length() - 2, buf.length());
                }
            }
            tContext = context.copy();
            buf.delete(buf.length() - 2, buf.length());
            splitted.add(buf.toString());
            buf.delete(0, buf.length());
            while ( !endsWith(buf, "?>")) {
                int ch = context.read();
                if (ch == -1) {
                    context.addError("Expected \"?>\"");
                    return null;
                }
                buf.append((char) ch);
            }
            buf.delete(buf.length() - 2, buf.length());
            String com = buf.toString().replace("\n", "");
            buf.delete(0, buf.length());
            Matcher m = CONTROL_PATTERN.matcher(com);
            if (m.matches()) {
                String type = m.group(1);
                String variable = m.group(2);
                ParseContext bodyContext = tContext.copy();
                ParseResult body = parseContent(bodyContext);
                if (type.equals("if")) {
                    if ("else".equals(body.getEndType())) {
                        ParseContext bodyContext2 = bodyContext.copy();
                        commands.add(new IfStatement(variable, body.getBlock("else"), parseContent(bodyContext).getBlock("}")));
                        bodyContext.merge(bodyContext2);
                    } else {
                        commands.add(new IfStatement(variable, body.getBlock("}")));
                    }
                } else if (type.equals("foreach")) {
                    commands.add(new ForeachStatement(variable, body.getBlock("}")));
                } else {
                    String bestMatching = EditDistance.getBestMatchingStringByEditDistance(type, POSSIBLE_CONTROL_PATTERNS);
                    tContext.addError(String.format(UNKOWN_CONTROL_STRUCTURE_MSG, type, bestMatching));
                }
                tContext.merge(bodyContext);
                continue;
            } else if ((m = ELSE_PATTERN.matcher(com)).matches()) {
                blockType = "else";
                break;
            } else if (com.matches(" ?\\} ?")) {
                blockType = "}";
                break;
            } else {
                commands.add(parseCommand(com, tContext));
            }
        }
        if (tContext != null) {
            context.merge(tContext);
        }
        splitted.add(buf.toString());
        ParseResult result = new ParseResult(new TemplateBlock(splitted.toArray(new String[splitted.size()]), commands.toArray(new Translatable[commands.size()])), blockType);
        return result;
    }

    private boolean endsWith(StringBuffer buf, String string) {
        return buf.length() >= string.length() && buf.substring(buf.length() - string.length(), buf.length()).equals(string);
    }

    private Translatable parseCommand(String s2, ParseContext context) {
        if (s2.startsWith("=_")) {
            final String raw = s2.substring(2);
            if ( !s2.contains("$") && !s2.contains("!'")) {
                return new TranslateCommand(raw);
            } else {
                return new SprintfCommand(raw);
            }
        } else if (s2.startsWith("=$")) {
            final String raw = s2.substring(2);
            return new OutputVariableCommand(raw);
        } else {
            context.addError("Unknown processing instruction \"" + s2 + "\"," + " did you mean \"" + EditDistance.getBestMatchingStringByEditDistance(s2, new String[] {
                    "=_", "=$"
            }) + "\"?");
            return null;
        }
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        tryReload();
        data.output(out, l, vars);
    }

    protected void tryReload() {
        if (source != null && lastLoaded < source.lastModified()) {
            System.out.println("Reloading template.... " + source);
            try (FileInputStream fis = new FileInputStream(source); InputStreamReader r = new InputStreamReader(fis, "UTF-8")) {
                data = parse(r).getBlock(null);
                r.close();
                lastLoaded = source.lastModified() + 1000;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected static void outputVar(PrintWriter out, Language l, Map<String, Object> vars, String varname, boolean unescaped) {
        if (vars.containsKey(Outputable.OUT_KEY_PLAIN)) {
            unescaped = true;
        }
        Object s = vars.get(varname);

        if (s == null) {
            System.err.println("Empty variable: " + varname);
        }
        if (s instanceof Outputable) {
            ((Outputable) s).output(out, l, vars);
        } else if (s instanceof DayDate) {
            out.print(DateSelector.getDateFormat().format(((DayDate) s).toDate()));
        } else if (s instanceof Boolean) {
            out.print(((Boolean) s) ? l.getTranslation("yes") : l.getTranslation("no"));
        } else if (s instanceof Date) {
            SimpleDateFormat sdfUI = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (vars.containsKey(Outputable.OUT_KEY_PLAIN)) {
                out.print(sdfUI.format(s));
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                out.print("<time datetime=\"" + sdf.format(s) + "\">");
                out.print(sdfUI.format(s));
                out.print(" UTC</time>");
            }
        } else {
            out.print(s == null ? "null" : (unescaped ? s.toString() : HTMLEncoder.encodeHTML(s.toString())));
        }
    }

    public void addTranslations(Collection<String> s) {
        data.addTranslations(s);
    }

    private class ParseContext {

        public static final int CONTEXT_LENGTH = 20;

        private Reader reader;

        public final TemplateParseException parseException = new TemplateParseException(source);

        int line = 1;

        int column = 0;

        private int curChar = -1;

        private int[] charContext = new int[CONTEXT_LENGTH];

        protected int contextPosition = 0;

        public ParseContext(Reader reader) {
            this.reader = reader;
        }

        public void addError(String message) {
            addError(line, column, message);
        }

        public void addError(int line, int column, String message) {
            StringBuffer charContextBuffer = new StringBuffer();
            int j = contextPosition;
            for (int i = 0; i < CONTEXT_LENGTH; i++) {
                if (charContext[j] != 0) {
                    if (charContext[j] == '\n') {
                        charContextBuffer.append("\\n");
                    } else {
                        charContextBuffer.appendCodePoint(charContext[j]);
                    }
                }
                j = (j + 1) % CONTEXT_LENGTH;
            }
            parseException.addError(line, column, message, charContextBuffer.toString());
        }

        public void merge(ParseContext other) {
            line = other.line;
            column = other.column;
            append(other);
        }

        public void append(ParseContext other) {
            parseException.append(other.parseException);
        }

        public int read() throws IOException {
            int ch;
            while ((ch = reader.read()) == '\r') {
            }
            curChar = ch;
            if (ch == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
            if (ch != -1) {
                charContext[contextPosition] = ch;
                contextPosition = (contextPosition + 1) % CONTEXT_LENGTH;
            }
            return ch;
        }

        public ParseContext copy() {
            ParseContext newParseContext = new ParseContext(reader);
            newParseContext.line = line;
            newParseContext.column = column;
            newParseContext.charContext = Arrays.copyOf(charContext, charContext.length);
            newParseContext.contextPosition = contextPosition;
            return newParseContext;
        }
    }
}
