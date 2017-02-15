package club.wpia.gigi.output.template;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * A exception that is thrown when a template contains syntactic errors. It
 * allows the combining of several error messages to catch more than one error
 * in a template.
 */
public class TemplateParseException extends IOException {

    private static final long serialVersionUID = 1L;

    private Object templateSource;

    private Set<ErrorMessage> errors = new TreeSet<>();

    public TemplateParseException(Object templateSource) {
        this.templateSource = templateSource;
    }

    public void addError(ErrorMessage error) {
        errors.add(error);
    }

    public void addError(int line, int column, String message, String erroneousLine) {
        addError(new ErrorMessage(line, column, message, erroneousLine));
    }

    public void append(TemplateParseException other) {
        errors.addAll(other.errors);
    }

    @Override
    public String toString() {
        StringBuilder strb = new StringBuilder("Error in template \"");
        strb.append(templateSource);
        strb.append("\":");
        for (ErrorMessage errorMessage : errors) {
            strb.append("\n\t");
            strb.append(errorMessage.toString());
        }
        return strb.toString();
    }

    @Override
    public String getMessage() {
        return toString();
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public static class ErrorMessage implements Comparable<ErrorMessage> {

        private final int line;

        private final int column;

        private final String message;

        private final String erroneousLine;

        public ErrorMessage(int line, int column, String message, String erroneousLine) {
            this.line = line;
            this.column = column;
            this.message = message;
            this.erroneousLine = erroneousLine;
        }

        @Override
        public String toString() {
            return String.format("Around %d:%d (after …%s…) %s", line, column, erroneousLine, message);
        }

        @Override
        public int compareTo(ErrorMessage o) {
            int l = Integer.compare(line, o.line);
            if (l != 0) {
                return l;
            }
            return Integer.compare(column, o.column);
        }
    }

}
