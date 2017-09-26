package club.wpia.gigi.localisation;

import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ExtendedStringLiteral;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import club.wpia.gigi.database.DBEnum;
import club.wpia.gigi.database.DatabaseConnection;
import club.wpia.gigi.database.GigiPreparedStatement;

public class SQLTestingVisitor extends ASTVisitor {

    private CompilationUnitDeclaration pu;

    private TranslationCollector tc;

    private enum Type {
        TIMESTAMP(Types.TIMESTAMP), ENUM(Types.VARCHAR), STRING(Types.VARCHAR), BOOLEAN(Types.BOOLEAN), DATE(Types.DATE), INTEGER(Types.INTEGER), OTHER(0);

        private final int sqltype;

        private Type(int sqltype) {
            this.sqltype = sqltype;

        }

        public void set(GigiPreparedStatement ps, int index) {
            if (this == TIMESTAMP) {
                ps.setTimestamp(index, new Timestamp(System.currentTimeMillis()));
            } else if (this == STRING) {
                ps.setString(index, "y");
            } else if (this == DATE) {
                ps.setDate(index, new Date(System.currentTimeMillis()));
            } else if (this == Type.BOOLEAN) {
                ps.setBoolean(index, false);
            } else if (this == OTHER || this == INTEGER) {
                ps.setInt(index, 1000);
            } else {
                throw new Error();
            }
        }

        public boolean isOfSQLType(int i) {
            if (i == sqltype) {
                return true;
            }
            if (i == Types.BIT && this == BOOLEAN) {
                return true;
            }
            return false;
        }
    }

    private class TypeInstantiation {

        Type type;

        String enumValue;

        public TypeInstantiation(Type type) {
            this.type = type;
        }

        public TypeInstantiation(Type type, String enumValue) {
            this.enumValue = enumValue;
            this.type = type;
        }

        public void set(GigiPreparedStatement ps, int index) {
            if (type == Type.ENUM) {
                ps.setString(index, enumValue);
            } else {
                type.set(ps, index);
            }
        }

        @Override
        public String toString() {
            return type.toString() + (enumValue != null ? enumValue : "");
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((enumValue == null) ? 0 : enumValue.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TypeInstantiation other = (TypeInstantiation) obj;
            if ( !getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (enumValue == null) {
                if (other.enumValue != null) {
                    return false;
                }
            } else if ( !enumValue.equals(other.enumValue)) {
                return false;
            }
            if (type != other.type) {
                return false;
            }
            return true;
        }

        private SQLTestingVisitor getOuterType() {
            return SQLTestingVisitor.this;
        }

    }

    public class SQLOccurrence {

        private List<String> query;

        private TryStatement target;

        private CompilationResult source;

        public TypeInstantiation[] types = new TypeInstantiation[10];

        private int sourceStart;

        public SQLOccurrence(TryStatement target) {
            this.target = target;
        }

        public void setQuery(List<String> query, CompilationResult compilationResult, int sourceStart) {
            this.query = query;
            this.source = compilationResult;
            this.sourceStart = sourceStart;
        }

        public TryStatement getTarget() {
            return target;
        }

        public List<String> getQuery() {
            return query;
        }

        public int getSourceStart() {
            return sourceStart;
        }

        public boolean isQuery() {
            return query != null;
        }

        public String getPosition() {
            int pos = source.lineSeparatorPositions.length + 1;
            for (int i = 0; i < source.lineSeparatorPositions.length; i++) {
                if (source.lineSeparatorPositions[i] > sourceStart) {
                    pos = i + 1;
                    break;
                }
            }
            return new String(source.getFileName()) + ":" + pos;
        }

        private void check(String stmt) {
            tc.countStatement();
            try (DatabaseConnection.Link l = DatabaseConnection.newLink(true)) {
                try (GigiPreparedStatement ps = new GigiPreparedStatement(stmt)) {
                    ParameterMetaData dt = ps.getParameterMetaData();
                    int count = dt.getParameterCount();
                    for (int i = 1; i <= types.length; i++) {
                        if (i > count) {
                            if (types[i - 1] != null) {
                                errMsg(stmt, "too many params");
                                return;
                            }
                            continue;
                        }
                        int tp = dt.getParameterType(i);
                        TypeInstantiation t = types[i - 1];
                        if (t == null) {
                            errMsg(stmt, "arg " + i + " not set");
                            return;
                        }
                        if ( !t.type.isOfSQLType(tp)) {
                            errMsg(stmt, "type mismatch. From parameter setting code: " + t + ", in SQL statement: " + tp);
                            return;
                        }
                    }
                } catch (SQLException e) {
                    errMsg(stmt, "SQL exception occurred, probably a syntax error in the SQL statement. See exception for more details.");
                    throw new Error(e);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void errMsg(String stmt, String errMsg) {
            System.err.println(getPosition());
            System.err.println("Problem with statement: " + stmt);
            System.err.println(Arrays.toString(types));
            System.err.println(errMsg);
            tc.hadError();
        }

        public void check() {
            for (String q : getQuery()) {
                check(q);
            }

        }
    }

    public SQLTestingVisitor(CompilationUnitDeclaration pu, TranslationCollector tc) {
        this.pu = pu;
        this.tc = tc;
    }

    Deque<SQLOccurrence> ts = new LinkedBlockingDeque<>();

    @Override
    public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
        return true;
    }

    @Override
    public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
        return true;
    }

    @Override
    public boolean visit(TryStatement tryStatement, BlockScope scope) {
        ts.push(new SQLOccurrence(tryStatement));
        return true;
    }

    @Override
    public void endVisit(TryStatement tryStatement, BlockScope scope) {
        SQLOccurrence occ = ts.pop();
        if (occ.isQuery()) {
            occ.check();
        }
        if (occ.getTarget() != tryStatement) {
            throw new Error();
        }
    }

    @Override
    public boolean visit(AllocationExpression ae, BlockScope scope) {
        MethodBinding mb = ae.binding;
        if (new String(mb.declaringClass.qualifiedPackageName()).equals("club.wpia.gigi.database") && new String(mb.declaringClass.qualifiedSourceName()).equals("GigiPreparedStatement")) {
            String sig = new String(mb.readableName());
            if (sig.equals("GigiPreparedStatement(String)") || sig.equals("GigiPreparedStatement(String, boolean)")) {
                List<String> l = getQueries(ae.arguments[0], scope);
                if (l.size() == 0) {
                    return false;
                }
                LinkedList<String> qs = new LinkedList<>();
                for (String q : l) {
                    qs.add(DatabaseConnection.preprocessQuery(q));
                }
                ts.peek().setQuery(qs, scope.compilationUnitScope().referenceContext.compilationResult, ae.sourceStart);
            } else {
                throw new Error(sig);
            }
        }
        return true;
    }

    private List<String> getQueries(Expression q, BlockScope scope) {
        SourceTypeBinding typ = scope.enclosingSourceType();
        String fullType = new String(typ.qualifiedPackageName()) + "." + new String(typ.qualifiedSourceName());
        if (fullType.equals("club.wpia.gigi.database.IntegrityVerifier")) {
            return Arrays.asList();
        }
        if (q instanceof StringLiteral) {
            String s = new String(((StringLiteral) q).source());
            return Arrays.asList(s);
        } else if (q instanceof ExtendedStringLiteral) {
            throw new Error();
        } else if (q instanceof BinaryExpression) {
            Expression l = ((BinaryExpression) q).left;
            Expression r = ((BinaryExpression) q).right;
            if ( !((BinaryExpression) q).operatorToString().equals("+")) {
                throw new Error(((BinaryExpression) q).operatorToString());
            }
            List<String> left = getQueries(l, scope);
            List<String> right = getQueries(r, scope);
            LinkedList<String> res = new LinkedList<>();
            for (String leftS : left) {
                for (String rightS : right) {
                    res.add(leftS + rightS);
                }
            }
            return res;
        } else if (q instanceof ConditionalExpression) {
            Expression t = ((ConditionalExpression) q).valueIfTrue;
            Expression f = ((ConditionalExpression) q).valueIfFalse;
            List<String> res = new LinkedList<>();
            res.addAll(getQueries(t, scope));
            res.addAll(getQueries(f, scope));
            return res;
        } else if (q instanceof SingleNameReference) {
            SingleNameReference ref = (SingleNameReference) q;
            Constant c = ref.constant;
            if (c.equals(Constant.NotAConstant)) {
                throw new Error(q.toString());
            }
            return Arrays.asList(c.stringValue());
        } else {
            System.err.println(q.getClass() + ";" + q.toString());
            throw new Error(q.toString());
        }
    }

    @Override
    public boolean visit(MessageSend messageSend, BlockScope scope) {
        Expression r = messageSend.receiver;
        String rec = new String(r.resolvedType.readableName());
        if (rec.equals("club.wpia.gigi.database.GigiPreparedStatement")) {
            String selector = new String(messageSend.selector);
            if (selector.startsWith("set")) {
                SQLOccurrence peek = ts.peek();
                if (peek == null) {
                    throw new Error("setting parameter at bad location");
                }
                IntLiteral i = (IntLiteral) messageSend.arguments[0];
                int val = i.constant.intValue();
                TypeInstantiation typeInstantiation = getTypeInstantiation(messageSend, selector);
                if (peek.types[val - 1] != null && !peek.types[val - 1].equals(typeInstantiation)) {
                    throw new Error("multiple different typeInstantiations");
                }
                peek.types[val - 1] = typeInstantiation;
            }
        }
        return true;
    }

    private TypeInstantiation getTypeInstantiation(MessageSend messageSend, String selector) throws Error {
        switch (selector) {
        case "setTimestamp":
            return new TypeInstantiation(Type.TIMESTAMP);
        case "setEnum":
            TypeBinding rn = messageSend.arguments[1].resolvedType;
            String sn = new String(rn.qualifiedSourceName());
            String enumClass = new String(rn.readableName());
            enumClass = enumClass.substring(0, enumClass.length() - sn.length()) + sn.replace('.', '$');
            String dbn;
            try {
                dbn = ((DBEnum) ((Object[]) Class.forName(enumClass).getMethod("values").invoke(null))[0]).getDBName();
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
            return new TypeInstantiation(Type.ENUM, dbn);
        case "setString":
            return new TypeInstantiation(Type.STRING);
        case "setDate":
            return new TypeInstantiation(Type.DATE);
        case "setInt":
            return new TypeInstantiation(Type.INTEGER);
        case "setBoolean":
            return new TypeInstantiation(Type.BOOLEAN);
        default:
            return new TypeInstantiation(Type.OTHER);
        }
    }
}
