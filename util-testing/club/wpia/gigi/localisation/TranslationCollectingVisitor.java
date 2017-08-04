package club.wpia.gigi.localisation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Stack;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.util.Util;

public final class TranslationCollectingVisitor extends ASTVisitor {

    MethodBinding cm;

    private CompilationUnitDeclaration unit;

    TaintSource[] ts;

    private TranslationCollector translationCollector;

    Stack<QualifiedAllocationExpression> anonymousConstructorCall = new Stack<>();

    public TranslationCollectingVisitor(CompilationUnitDeclaration unit, TaintSource[] target, TranslationCollector c) {
        this.unit = unit;
        ts = target;
        this.translationCollector = c;
    }

    @Override
    public boolean visit(MethodDeclaration methodDeclaration, org.eclipse.jdt.internal.compiler.lookup.ClassScope scope) {
        cm = methodDeclaration.binding;
        return true;
    }

    @Override
    public void endVisit(MethodDeclaration methodDeclaration, org.eclipse.jdt.internal.compiler.lookup.ClassScope scope) {
        cm = null;
    }

    @Override
    public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
        cm = constructorDeclaration.binding;
        return super.visit(constructorDeclaration, scope);
    }

    @Override
    public void endVisit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
        cm = null;
    }

    @Override
    public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
        TaintSource test = new TaintSource(allocationExpression.binding);
        for (TaintSource taintSource : ts) {
            if (taintSource.equals(test)) {
                check(null, scope, allocationExpression.arguments[taintSource.getTgt()], allocationExpression.toString());
                return true;
            }
        }
        return super.visit(allocationExpression, scope);
    }

    @Override
    public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
        anonymousConstructorCall.push(qualifiedAllocationExpression);
        return super.visit(qualifiedAllocationExpression, scope);
    }

    @Override
    public void endVisit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
        if (anonymousConstructorCall.pop() != qualifiedAllocationExpression) {
            throw new Error("stack illegally manipulated");
        }
    }

    @Override
    public boolean visit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {

        TaintSource t = new TaintSource(explicitConstructor.binding);

        for (TaintSource t0 : ts) {
            if (t0.equals(t)) {
                Expression[] ags = explicitConstructor.arguments;
                if (anonymousConstructorCall.size() > 0) {
                    ags = anonymousConstructorCall.peek().arguments;
                }
                if (ags == null) {
                    System.err.println(explicitConstructor);
                    return true;
                }
                Expression e = ags[t0.getTgt()];
                check(null, scope, e, explicitConstructor.toString());
                break;
            }
        }
        return super.visit(explicitConstructor, scope);
    }

    @Override
    public boolean visit(org.eclipse.jdt.internal.compiler.ast.MessageSend call, org.eclipse.jdt.internal.compiler.lookup.BlockScope scope) {
        if (call.binding == null) {
            System.err.println("Unbound:" + call + " in " + call.sourceStart());
            return true;
        }
        // System.out.println("Message");
        TaintSource t = new TaintSource(call.binding);

        for (TaintSource t0 : ts) {
            if (t0.equals(t)) {
                Expression[] ags = call.arguments;
                if (ags == null) {
                    System.err.println(call);
                    return true;
                }
                Expression e = ags[t0.getTgt()];
                check(call, scope, e, call.toString());
                break;
            }
        }
        return true;
    }

    private void check(org.eclipse.jdt.internal.compiler.ast.MessageSend call, org.eclipse.jdt.internal.compiler.lookup.BlockScope scope, Expression e, String caller) {
        if (e instanceof StringLiteral) {
            int[] lineEnds = null;
            int lineNumber = Util.getLineNumber(e.sourceStart, lineEnds = unit.compilationResult.getLineSeparatorPositions(), 0, lineEnds.length - 1);

            String content = new String(((StringLiteral) e).source());
            File f0 = new File(new String(unit.compilationResult.fileName)).getAbsoluteFile();
            File f2 = translationCollector.base.getAbsoluteFile();
            try {
                translationCollector.add(content, f0.getCanonicalPath().substring(f2.getCanonicalPath().length() + 1) + ":" + lineNumber);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }

        if (e instanceof NullLiteral) {
            return;
        }

        if (e instanceof MessageSend) {
            MessageSend m2 = (MessageSend) e;
            TaintSource ts = new TaintSource(m2.binding);
            if (ts.equals(new TaintSource("club.wpia.gigi.pages", "Page", "getTitle()", 0))) {
                return;
            }
            if (m2.receiver.resolvedType.isCompatibleWith(scope.getJavaLangEnum())) {
                testEnum(m2.receiver, m2.binding);
                System.err.println("ENUM-SRC: !" + m2.receiver);
            }
        }
        if (e.resolvedType.isCompatibleWith(scope.getJavaLangEnum())) {
            // TODO ?
            System.err.println("ENUM-Not-Hanled");
        }

        TaintSource b = cm == null ? null : new TaintSource(cm);
        for (TaintSource taintSource : ts) {
            if (taintSource.equals(b) || (taintSource.getMaskOnly() != null && taintSource.getMaskOnly().equals(b))) {
                return;
            }
        }
        if (e instanceof ConditionalExpression) {
            check(call, scope, ((ConditionalExpression) e).valueIfFalse, caller);
            check(call, scope, ((ConditionalExpression) e).valueIfTrue, caller);
            return;
        }

        System.err.println();

        System.err.println(new String(scope.enclosingClassScope().referenceType().compilationResult.fileName));
        System.err.println("Cannot Handle: " + e + " in " + (call == null ? "constructor" : call.sourceStart) + " => " + caller);
        System.err.println(e.getClass());
        System.err.println("To ignore: " + (b == null ? "don't know" : b.toConfLine()));
        translationCollector.hadError();
    }

    private void testEnum(Expression e, MethodBinding binding) {
        if (binding.parameters.length != 0) {
            System.err.println("ERROR: meth");
            return;
        }
        System.err.println(e.resolvedType.getClass());
        String s2 = new String(e.resolvedType.qualifiedPackageName()) + "." + (new String(e.resolvedType.qualifiedSourceName()).replace('.', '$'));
        try {
            Class<?> c = Class.forName(s2);
            Enum<?>[] e1 = (Enum[]) c.getMethod("values").invoke(null);
            Method m = c.getMethod(new String(binding.selector));
            for (int j = 0; j < e1.length; j++) {
                System.err.println(m.invoke(e1[j]));
            }
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        } catch (ReflectiveOperationException e1) {
            e1.printStackTrace();
        }
        System.err.println("ENUM-done: " + e + "!");
        return;
    }
}
