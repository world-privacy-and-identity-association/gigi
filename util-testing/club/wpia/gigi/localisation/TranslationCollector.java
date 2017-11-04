package club.wpia.gigi.localisation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.ISourceType;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.ITypeRequestor;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

import club.wpia.gigi.database.DatabaseConnection;
import club.wpia.gigi.output.template.Template;

public class TranslationCollector {

    static class TranslationEntry implements Comparable<TranslationEntry> {

        String text;

        String occur1;

        List<String> occur;

        public TranslationEntry(String text, String occur) {
            this.text = text;
            occur1 = occur;
        }

        public List<String> getOccur() {
            if (occur == null) {
                return Arrays.asList(occur1);
            }
            return occur;
        }

        public void add(String t) {
            if (occur == null) {
                occur = new ArrayList<>(Arrays.asList(occur1));
            }
            occur.add(t);
        }

        @Override
        public int compareTo(TranslationEntry o) {
            int i = occur1.compareTo(o.occur1);
            if (i != 0) {
                return i;
            }

            return text.compareTo(o.text);
        }
    }

    private HashMap<String, TranslationEntry> translations = new HashMap<>();

    public final File base;

    private boolean hadErrors = false;

    private int statements = 0;

    public TranslationCollector(File base, File conf) {
        this.base = base;
        taint = new LinkedList<>();
        for (String s : new FileIterable(conf)) {
            taint.add(TaintSource.parseTaint(s));
        }
    }

    interface ASTVisitorFactory {

        public ASTVisitor createVisitor(CompilationUnitDeclaration parsedUnit);
    }

    public void run(File out) throws IOException {
        scanTemplates();
        scanCode(new ASTVisitorFactory() {

            @Override
            public ASTVisitor createVisitor(CompilationUnitDeclaration parsedUnit) {
                return new TranslationCollectingVisitor(parsedUnit, taint.toArray(new TaintSource[taint.size()]), TranslationCollector.this);
            }
        });

        System.err.println("Total Translatable Strings: " + translations.size());
        TreeSet<TranslationEntry> trs = new TreeSet<>(translations.values());
        writePOFile(out, trs);
    }

    public void runSQLValidation() throws IOException {
        scanCode(new ASTVisitorFactory() {

            @Override
            public ASTVisitor createVisitor(CompilationUnitDeclaration parsedUnit) {
                return new SQLTestingVisitor(parsedUnit, TranslationCollector.this);
            }
        });
        System.out.println("Validated: " + statements + " SQL statements.");
    }

    public void add(String text, String line) {
        if (text.contains("\r") || text.contains("\n")) {
            throw new Error("Malformed translation in " + line);
        }
        TranslationEntry i = translations.get(text);
        if (i == null) {
            translations.put(text, new TranslationEntry(text, line));
            return;
        }
        i.add(line);
    }

    private void scanCode(ASTVisitorFactory visitor) throws Error {
        PrintWriter out = new PrintWriter(System.err);
        Main m = new Main(out, out, false, null, null);
        File[] fs = recurse(new File(new File(new File(base, "src"), "club"), "wpia"), new LinkedList<File>(), ".java").toArray(new File[0]);
        String[] t = new String[fs.length + 3];
        t[0] = "-cp";
        t[1] = new File(base, "bin").getAbsolutePath();
        t[2] = "-7";
        for (int i = 0; i < fs.length; i++) {
            t[i + 3] = fs[i].getAbsolutePath();
        }
        m.configure(t);
        FileSystem environment = m.getLibraryAccess();
        CompilerOptions compilerOptions = new CompilerOptions(m.options);
        compilerOptions.performMethodsFullRecovery = false;
        compilerOptions.performStatementsRecovery = false;
        // check
        compilerOptions.sourceLevel = ClassFileConstants.JDK1_7;
        compilerOptions.complianceLevel = ClassFileConstants.JDK1_7;
        compilerOptions.originalComplianceLevel = ClassFileConstants.JDK1_7;

        ProblemReporter pr = new ProblemReporter(m.getHandlingPolicy(), compilerOptions, m.getProblemFactory());
        ITypeRequestor tr = new ITypeRequestor() {

            @Override
            public void accept(ISourceType[] sourceType, PackageBinding packageBinding, AccessRestriction accessRestriction) {
                throw new IllegalStateException("source type not implemented");
            }

            @Override
            public void accept(IBinaryType binaryType, PackageBinding packageBinding, AccessRestriction accessRestriction) {
                le.createBinaryTypeFrom(binaryType, packageBinding, accessRestriction);
            }

            @Override
            public void accept(ICompilationUnit unit, AccessRestriction accessRestriction) {
                throw new IllegalStateException("compilation unit not implemented");
            }
        };
        le = new LookupEnvironment(tr, compilerOptions, pr, environment);
        Parser parser = new Parser(pr, compilerOptions.parseLiteralExpressionsAsConstants);
        CompilationUnit[] sourceUnits = m.getCompilationUnits();
        CompilationUnitDeclaration[] parsedUnits = new CompilationUnitDeclaration[sourceUnits.length];
        for (int i = 0; i < parsedUnits.length; i++) {

            CompilationResult unitResult = new CompilationResult(sourceUnits[i], i, parsedUnits.length, compilerOptions.maxProblemsPerUnit);
            CompilationUnitDeclaration parsedUnit = parser.parse(sourceUnits[i], unitResult);
            le.buildTypeBindings(parsedUnit, null /* no access restriction */);
            parsedUnits[i] = parsedUnit;
        }
        le.completeTypeBindings();
        for (int i = 0; i < parsedUnits.length; i++) {
            CompilationUnitDeclaration parsedUnit = parsedUnits[i];

            parser.getMethodBodies(parsedUnit);
            parsedUnit.scope.faultInTypes();
            parsedUnit.scope.verifyMethods(le.methodVerifier());
            parsedUnit.resolve();
        }
        for (int i = 0; i < parsedUnits.length; i++) {
            CompilationUnitDeclaration parsedUnit = parsedUnits[i];
            if (parsedUnit.compilationResult.problems != null) {
                int err = 0;
                for (int c = 0; c < parsedUnit.compilationResult.problemCount; c++) {
                    CategorizedProblem problem = parsedUnit.compilationResult.problems[c];
                    if (problem.isError()) {
                        err++;
                    }
                    if (OUTPUT_WARNINGS || problem.isError()) {
                        System.err.println(problem);
                        StringBuilder prob = new StringBuilder();
                        prob.append(parsedUnit.compilationResult.fileName);
                        prob.append(":");
                        prob.append(problem.getSourceLineNumber());
                        System.err.println(prob.toString());
                    }
                }
                if (err > 0) {
                    throw new Error();
                }
            }
            // Skip Database connection as some statements need to be run on older DB scheme version.
            if(new String(parsedUnit.getFileName()).endsWith("/src/club/wpia/gigi/database/DatabaseConnection.java")) {
                continue;
            }
            if (parsedUnit.types == null) {
                System.err.println("No types");

            } else {
                ASTVisitor v = visitor.createVisitor(parsedUnit);
                for (TypeDeclaration td : parsedUnit.types) {
                    td.traverse(v, td.scope);
                }
            }
            parsedUnits[i] = parsedUnit;
        }
    }

    private void scanTemplates() throws UnsupportedEncodingException, FileNotFoundException {
        File[] ts = recurse(new File(new File(new File(base, "src"), "club"), "wpia"), new LinkedList<File>(), ".templ").toArray(new File[0]);
        for (File file : ts) {
            Template t = new Template(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            LinkedList<String> i = new LinkedList<String>();
            t.addTranslations(i);
            for (String string : i) {
                add(string, file.getAbsolutePath().substring(base.getAbsolutePath().length() + 1) + ":1");
            }
        }
    }

    static LookupEnvironment le;

    private static final boolean OUTPUT_WARNINGS = false;

    private LinkedList<TaintSource> taint;

    public static void main(String[] args) throws IOException {
        Properties pp = new Properties();
        pp.load(new InputStreamReader(new FileInputStream("config/test.properties"), "UTF-8"));
        DatabaseConnection.init(pp);
        TranslationCollector tc = new TranslationCollector(new File(args[1]), new File(args[0]));
        if (args[2].equals("SQLValidation")) {
            tc.runSQLValidation();
        } else {
            tc.run(new File(args[2]));
        }
        if (tc.hadErrors) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    public static void writePOFile(File target, Collection<TranslationEntry> strings) throws IOException {
        PrintWriter out = new PrintWriter(target);
        for (TranslationEntry s : strings) {
            out.print("#:");
            for (String st : s.getOccur()) {
                out.print(" " + st);
            }
            out.println();
            out.println("msgid \"" + s.text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
            out.println("msgstr \"\"");
            out.println();
        }
        out.close();
    }

    private static List<File> recurse(File file, List<File> toAdd, String pt) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                recurse(f, toAdd, pt);
            }
        } else {
            if (file.getName().endsWith(pt)) {
                toAdd.add(file);
            }
        }
        return toAdd;
    }

    public void hadError() {
        hadErrors = true;
    }

    public void countStatement() {
        statements++;
    }
}
