package de.aschoerk.javaconv;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.DumpVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.github.javaparser.PositionUtils.sortByBeginPosition;
import static com.github.javaparser.ast.internal.Utils.isNullOrEmpty;

/*
        * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
        * Copyright (C) 2011, 2013-2015 The JavaParser Team.
        *
        * This file is part of JavaParser.
        *
        * JavaParser can be used either under the terms of
        * a) the GNU Lesser General Public License as published by
        *     the Free Software Foundation, either version 3 of the License, or
        *     (at your option) any later version.
        * b) the terms of the Apache License
        *
        * You should have received a copy of both licenses in LICENCE.LGPL and
        * LICENCE.APACHE. Please refer to those files for details.
        *
        * JavaParser is distributed in the hope that it will be useful,
        * but WITHOUT ANY WARRANTY; without even the implied warranty of
        * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        * GNU Lesser General Public License for more details.
        */
@SuppressWarnings("Duplicates")

/**
 * Dumps the AST to formatted Java source code.
 *
 * @author Julio Vilmar Gesser
 */
public class RustDumpVisitor extends VoidVisitorAdapter<Object> {

        private boolean printComments;

        public RustDumpVisitor() {
            this(true);
        }

        public RustDumpVisitor(boolean printComments) {
            this.printComments = printComments;
        }

        public static class SourcePrinter {

            private final String indentation;

            public SourcePrinter(final String indentation) {
                this.indentation = indentation;
            }

            private int level = 0;

            private boolean indented = false;

            private final StringBuilder buf = new StringBuilder();

            private List<Integer> marks = new ArrayList<>();

            public void indent() {
                level++;
            }

            public void unindent() {
                level--;
            }

            private void makeIndent() {
                for (int i = 0; i < level; i++) {
                    buf.append(indentation);
                }
            }

            public void print(final String arg) {
                if (!indented) {
                    makeIndent();
                    indented = true;
                }
                buf.append(arg);
            }

            public void printLn(final String arg) {
                print(arg);
                printLn();
            }

            public void printLn() {
                buf.append(System.getProperty("line.separator"));
                indented = false;
            }

            public String getSource() {
                return buf.toString();
            }

            public int push() {
                marks.add(buf.length());
                return marks.size();
            }

            public String getMark(int mark) {
                return buf.substring(marks.get(mark - 1));
            }

            public void pop() {
                buf.delete(marks.get(marks.size() - 1), buf.length());
                marks.remove(marks.size()-1);
            }

            public void drop() {
                marks.remove(marks.size()-1);
            }

            @Override public String toString() {
                return getSource();
            }
        }

        protected final SourcePrinter printer = createSourcePrinter();

        protected SourcePrinter createSourcePrinter() {
            return new SourcePrinter("    ");
        }

        public String getSource() {
            return printer.getSource();
        }

    static String[] mappedNames = {
            "NaN", "NAN",
            "NEGATIVE_INFINITY", "NEG_INFINITY",
            "POSITIVE_INFINITY", "INFINITY",
            "MIN_VALUE","MIN",
            "MAX_VALUE","MAX",
    };

    static HashMap<String, String> namesMap = new HashMap<>();
    static {
        for (int i = 0; i < mappedNames.length; i += 2) {
            namesMap.put(mappedNames[i], mappedNames[i+1]);
        }
    }


    private String toSnakeIfNecessary(String n) {
        System.out.println("doing: " + n);
        if (namesMap.containsKey(n))
            n = namesMap.get(n);
        String name = n;
        if (Character.isLowerCase(name.charAt(0))) {
            StringBuilder sb = new StringBuilder();
            for (Character c: name.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    sb.append("_").append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        return n;
    }

    private String removePlusAndSuffix(String value,  CharSequence... searchStrings) {
        if (value.startsWith("+")) {
            value = value.substring(1);
        }
        if (StringUtils.endsWithAny(value, searchStrings)) {
            value = value.substring(0, value.length()-1);
        }
        return value;
    }


    protected void printModifiers(final int modifiers) {
            if (ModifierSet.isPrivate(modifiers)) {
                printer.print("private ");
            }
            if (ModifierSet.isProtected(modifiers)) {
                printer.print("protected ");
            }
            if (ModifierSet.isPublic(modifiers)) {
                printer.print("public ");
            }
            if (ModifierSet.isAbstract(modifiers)) {
                printer.print("abstract ");
            }
            if (ModifierSet.isStatic(modifiers)) {
                printer.print("static ");
            }
            if (ModifierSet.isFinal(modifiers)) {
                printer.print("final ");
            }
            if (ModifierSet.isNative(modifiers)) {
                printer.print("native ");
            }
            if (ModifierSet.isStrictfp(modifiers)) {
                printer.print("strictfp ");
            }
            if (ModifierSet.isSynchronized(modifiers)) {
                printer.print("synchronized ");
            }
            if (ModifierSet.isTransient(modifiers)) {
                printer.print("transient ");
            }
            if (ModifierSet.isVolatile(modifiers)) {
                printer.print("volatile ");
            }
        }

    protected void printMembers(final List<BodyDeclaration> members, final Object arg) {
            for (final BodyDeclaration member : members) {
                printer.printLn();
                member.accept(this, arg);
                printer.printLn();
            }
        }

    protected void printTypeArgs(final List<Type> args, final Object arg) {
            if (!isNullOrEmpty(args)) {
                printer.print("<");
                for (final Iterator<Type> i = args.iterator(); i.hasNext();) {
                    final Type t = i.next();
                    t.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
                printer.print(">");
            }
        }

    protected void printTypeParameters(final List<TypeParameter> args, final Object arg) {
            if (!isNullOrEmpty(args)) {
                printer.print("<");
                for (final Iterator<TypeParameter> i = args.iterator(); i.hasNext();) {
                    final TypeParameter t = i.next();
                    t.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
                printer.print(">");
            }
        }

    protected void printArguments(final List<Expression> args, final Object arg) {
            printer.print("(");
            if (!isNullOrEmpty(args)) {
                for (final Iterator<Expression> i = args.iterator(); i.hasNext();) {
                    final Expression e = i.next();
                    e.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
            }
            printer.print(")");
        }

    protected void printJavadoc(final JavadocComment javadoc, final Object arg) {
            if (javadoc != null) {
                javadoc.accept(this, arg);
            }
        }

        protected void printJavaComment(final Comment javacomment, final Object arg) {
            if (javacomment != null) {
                javacomment.accept(this, arg);
            }
        }

        @Override public void visit(final CompilationUnit n, final Object arg) {
            printJavaComment(n.getComment(), arg);

            if (n.getPackage() != null) {
                n.getPackage().accept(this, arg);
            }

            if (!isNullOrEmpty(n.getImports())) {
                for (final ImportDeclaration i : n.getImports()) {
                    i.accept(this, arg);
                }
                printer.printLn();
            }

            if (!isNullOrEmpty(n.getTypes())) {
                for (final Iterator<TypeDeclaration> i = n.getTypes().iterator(); i.hasNext();) {
                    i.next().accept(this, arg);
                    printer.printLn();
                    if (i.hasNext()) {
                        printer.printLn();
                    }
                }
            }

            printOrphanCommentsEnding(n);
        }

        @Override public void visit(final PackageDeclaration n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("package ");
            n.getName().accept(this, arg);
            printer.printLn(";");
            printer.printLn();

            printOrphanCommentsEnding(n);
        }

        @Override public void visit(final NameExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(toSnakeIfNecessary(n.getName()));

            printOrphanCommentsEnding(n);
        }

        @Override public void visit(final QualifiedNameExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            n.getQualifier().accept(this, arg);
            printer.print("::");
            printer.print(n.getName());

            printOrphanCommentsEnding(n);
        }

        @Override public void visit(final ImportDeclaration n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("import ");
            if (n.isStatic()) {
                printer.print("static ");
            }
            n.getName().accept(this, arg);
            if (n.isAsterisk()) {
                printer.print(".*");
            }
            printer.printLn(";");

            printOrphanCommentsEnding(n);
        }

        @Override public void visit(final ClassOrInterfaceDeclaration n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printJavadoc(n.getJavaDoc(), arg);
            printModifiers(n.getModifiers());

            if (n.isInterface()) {
                printer.print("interface ");
            } else {
                printer.print("class ");
            }

            printer.print(n.getName());

            printTypeParameters(n.getTypeParameters(), arg);

            if (!isNullOrEmpty(n.getExtends())) {
                printer.print(" extends ");
                for (final Iterator<ClassOrInterfaceType> i = n.getExtends().iterator(); i.hasNext();) {
                    final ClassOrInterfaceType c = i.next();
                    c.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
            }

            if (!isNullOrEmpty(n.getImplements())) {
                printer.print(" implements ");
                for (final Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
                    final ClassOrInterfaceType c = i.next();
                    c.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
            }

            printer.printLn(" {");
            printer.indent();
            if (!isNullOrEmpty(n.getMembers())) {
                printMembers(n.getMembers(), arg);
            }

            printOrphanCommentsEnding(n);

            printer.unindent();
            printer.print("}");
        }

        @Override public void visit(final EmptyTypeDeclaration n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printJavadoc(n.getJavaDoc(), arg);
            printer.print(";");

            printOrphanCommentsEnding(n);
        }

        @Override public void visit(final JavadocComment n, final Object arg) {
            printer.print("/**");
            printer.print(n.getContent());
            printer.printLn("*/");
        }

        @Override public void visit(final ClassOrInterfaceType n, final Object arg) {
            printJavaComment(n.getComment(), arg);

            if (n.getScope() != null) {
                n.getScope().accept(this, arg);
                printer.print(".");
            }
            printer.print(n.getName());

            if (n.isUsingDiamondOperator()) {
                printer.print("<>");
            } else {
                printTypeArgs(n.getTypeArgs(), arg);
            }
        }

        @Override public void visit(final TypeParameter n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(n.getName());
            if (!isNullOrEmpty(n.getTypeBound())) {
                printer.print(" extends ");
                for (final Iterator<ClassOrInterfaceType> i = n.getTypeBound().iterator(); i.hasNext();) {
                    final ClassOrInterfaceType c = i.next();
                    c.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(" & ");
                    }
                }
            }
        }

        @Override public void visit(final PrimitiveType n, final Object arg) {
            printJavaComment(n.getComment(), arg);

            switch (n.getType()) {
                case Boolean:
                    printer.print("bool");
                    break;
                case Byte:
                    printer.print("i8");
                    break;
                case Char:
                    printer.print("char");
                    break;
                case Double:
                    printer.print("f64");
                    break;
                case Float:
                    printer.print("f32");
                    break;
                case Int:
                    printer.print("i32");
                    break;
                case Long:
                    printer.print("i64");
                    break;
                case Short:
                    printer.print("i16");
                    break;
            }
        }

        @Override public void visit(final ReferenceType n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            n.getType().accept(this, arg);
            for (int i = 0; i < n.getArrayCount(); i++) {
                printer.print("[]");
            }
        }

        @Override public void visit(final IntersectionType n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            boolean isFirst = true;
            for (ReferenceType element : n.getElements()) {
                element.accept(this, arg);
                if (isFirst) {
                    isFirst = false;
                } else {
                    printer.print(" & ");
                }
            }
        }

        @Override public void visit(final UnionType n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            boolean isFirst = true;
            for (ReferenceType element : n.getElements()) {
                element.accept(this, arg);
                if (isFirst) {
                    isFirst = false;
                } else {
                    printer.print(" | ");
                }
            }
        }


        @Override public void visit(final WildcardType n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("?");
            if (n.getExtends() != null) {
                printer.print(" extends ");
                n.getExtends().accept(this, arg);
            }
            if (n.getSuper() != null) {
                printer.print(" super ");
                n.getSuper().accept(this, arg);
            }
        }

        @Override public void visit(final UnknownType n, final Object arg) {
            // Nothing to dump
        }

        @Override public void visit(final FieldDeclaration n, final Object arg) {
            printOrphanCommentsBeforeThisChildNode(n);

            printJavaComment(n.getComment(), arg);
            printJavadoc(n.getJavaDoc(), arg);
            printModifiers(n.getModifiers());
            n.getType().accept(this, arg);

            printer.print(" ");
            for (final Iterator<VariableDeclarator> i = n.getVariables().iterator(); i.hasNext();) {
                final VariableDeclarator var = i.next();
                var.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }

            printer.print(";");
        }

        @Override public void visit(final VariableDeclarator n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            n.getId().accept(this, arg);
            if (arg instanceof  Type && !(n.getInit() instanceof ArrayInitializerExpr)) {
                printer.print(": ");
                ((Type)arg).accept(this,arg);
            }
            if (n.getInit() != null) {
                printer.print(" = ");
                n.getInit().accept(this, arg);
            }
        }

        @Override public void visit(final VariableDeclaratorId n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(toSnakeIfNecessary(n.getName()));
        }

        @Override public void visit(final ArrayInitializerExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("{");
            if (!isNullOrEmpty(n.getValues())) {
                printer.print(" ");
                for (final Iterator<Expression> i = n.getValues().iterator(); i.hasNext();) {
                    final Expression expr = i.next();
                    expr.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
                printer.print(" ");
            }
            printer.print("}");
        }

        @Override public void visit(final VoidType n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("void");
        }

        @Override public void visit(final ArrayAccessExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            n.getName().accept(this, arg);
            printer.print("[");
            n.getIndex().accept(this, arg);
            printer.print("]");
        }

        @Override public void visit(final ArrayCreationExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            // printer.print("new ");
            // n.getType().accept(this, arg);
            if (!isNullOrEmpty(n.getDimensions())) {
                n.getType().accept(this, arg);
                int j = 0;
                for (final Expression dim : n.getDimensions()) {

                    printer.print("[");
                    dim.accept(this, arg);
                    printer.print("]");
                    j++;
                }
                for (int i = 0; i < n.getArrayCount(); i++) {
                    printer.print("[]");
                }
            } else {
                for (int i = 0; i < n.getArrayCount(); i++) {
                    printer.print("[]");
                }
                printer.print(" ");
                n.getInitializer().accept(this, n.getType());
            }
        }

        @Override public void visit(final AssignExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            n.getTarget().accept(this, arg);
            printer.print(" ");
            switch (n.getOperator()) {
                case assign:
                    printer.print("=");
                    break;
                case and:
                    printer.print("&=");
                    break;
                case or:
                    printer.print("|=");
                    break;
                case xor:
                    printer.print("^=");
                    break;
                case plus:
                    printer.print("+=");
                    break;
                case minus:
                    printer.print("-=");
                    break;
                case rem:
                    printer.print("%=");
                    break;
                case slash:
                    printer.print("/=");
                    break;
                case star:
                    printer.print("*=");
                    break;
                case lShift:
                    printer.print("<<=");
                    break;
                case rSignedShift:
                    printer.print(">>=");
                    break;
                case rUnsignedShift:
                    printer.print(">>= /* >>>= */");
                    break;
            }
            printer.print(" ");
            n.getValue().accept(this, arg);
        }

        @Override public void visit(final BinaryExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            n.getLeft().accept(this, arg);
            printer.print(" ");
            switch (n.getOperator()) {
                case or:
                    printer.print("||");
                    break;
                case and:
                    printer.print("&&");
                    break;
                case binOr:
                    printer.print("|");
                    break;
                case binAnd:
                    printer.print("&");
                    break;
                case xor:
                    printer.print("^");
                    break;
                case equals:
                    printer.print("==");
                    break;
                case notEquals:
                    printer.print("!=");
                    break;
                case less:
                    printer.print("<");
                    break;
                case greater:
                    printer.print(">");
                    break;
                case lessEquals:
                    printer.print("<=");
                    break;
                case greaterEquals:
                    printer.print(">=");
                    break;
                case lShift:
                    printer.print("<<");
                    break;
                case rSignedShift:
                    printer.print(">>");
                    break;
                case rUnsignedShift:
                    printer.print(">> /* >>> */");
                    break;
                case plus:
                    printer.print("+");
                    break;
                case minus:
                    printer.print("-");
                    break;
                case times:
                    printer.print("*");
                    break;
                case divide:
                    printer.print("/");
                    break;
                case remainder:
                    printer.print("%");
                    break;
            }
            printer.print(" ");
            n.getRight().accept(this, arg);
        }

        @Override public void visit(final CastExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("(");
            n.getType().accept(this, arg);
            printer.print(") ");
            n.getExpr().accept(this, arg);
        }

        @Override public void visit(final ClassExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            n.getType().accept(this, arg);
            printer.print(".class");
        }

        @Override public void visit(final ConditionalExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            n.getCondition().accept(this, arg);
            printer.print(" ? ");
            n.getThenExpr().accept(this, arg);
            printer.print(" : ");
            n.getElseExpr().accept(this, arg);
        }

        @Override public void visit(final EnclosedExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("(");
            if (n.getInner() != null) {
                n.getInner().accept(this, arg);
            }
            printer.print(")");
        }

        @Override public void visit(final FieldAccessExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            int mark = printer.push();
            n.getScope().accept(this, arg);
            String scope = printer.getMark(mark);
            printer.drop();
            int i = StringUtils.lastIndexOfAny(scope,"\n","\t"," ",".");
            String accessed = i <= 0 ? scope : scope.substring(i+1);
            if (Character.isUpperCase(accessed.charAt(0))) {
                printer.print("::");
            } else {
                printer.print(".");
            }
            printer.print(n.getField());
        }

        @Override public void visit(final InstanceOfExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            n.getExpr().accept(this, arg);
            printer.print(" instanceof ");
            n.getType().accept(this, arg);
        }

        @Override public void visit(final CharLiteralExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("'");
            printer.print(n.getValue());
            printer.print("'");
        }

        @Override public void visit(final DoubleLiteralExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            String value = n.getValue();
            if (!StringUtils.containsAny(value, '.','e','E','x','X'))
                value = value + ".0";
            printer.print(removePlusAndSuffix(value,"l","L"));
        }

        @Override public void visit(final IntegerLiteralExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(removePlusAndSuffix(n.getValue()));
        }

        @Override public void visit(final LongLiteralExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(removePlusAndSuffix(n.getValue(),"l","L"));
        }

        @Override public void visit(final IntegerLiteralMinValueExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(n.getValue());
        }

        @Override public void visit(final LongLiteralMinValueExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(n.getValue());
        }

        @Override public void visit(final StringLiteralExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("\"");
            printer.print(n.getValue());
            printer.print("\"");
        }

        @Override public void visit(final BooleanLiteralExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(String.valueOf(n.getValue()));
        }

        @Override public void visit(final NullLiteralExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("null");
        }

        @Override public void visit(final ThisExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            if (n.getClassExpr() != null) {
                n.getClassExpr().accept(this, arg);
                printer.print(".");
            }
            printer.print("this");
        }

        @Override public void visit(final SuperExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            if (n.getClassExpr() != null) {
                n.getClassExpr().accept(this, arg);
                printer.print(".");
            }
            printer.print("super");
        }

        @Override public void visit(final MethodCallExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            if (n.getScope() != null) {
                n.getScope().accept(this, arg);
                printer.print(".");
            }
            printTypeArgs(n.getTypeArgs(), arg);
            printer.print(toSnakeIfNecessary(n.getName()));
            printArguments(n.getArgs(), arg);
        }

        @Override public void visit(final ObjectCreationExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            if (n.getScope() != null) {
                n.getScope().accept(this, arg);
                printer.print(".");
            }

            printer.print("new ");

            printTypeArgs(n.getTypeArgs(), arg);
            if (!isNullOrEmpty(n.getTypeArgs())) {
                printer.print(" ");
            }

            n.getType().accept(this, arg);

            printArguments(n.getArgs(), arg);

            if (n.getAnonymousClassBody() != null) {
                printer.printLn(" {");
                printer.indent();
                printMembers(n.getAnonymousClassBody(), arg);
                printer.unindent();
                printer.print("}");
            }
        }

        @Override public void visit(final UnaryExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            switch (n.getOperator()) {
                case positive:
                    printer.print("+");
                    break;
                case negative:
                    printer.print("-");
                    break;
                case inverse:
                    printer.print("~");
                    break;
                case not:
                    printer.print("!");
                    break;
                case preIncrement:
                    printer.print("++");
                    break;
                case preDecrement:
                    printer.print("--");
                    break;
                default:
            }

            n.getExpr().accept(this, arg);

            switch (n.getOperator()) {
                case posIncrement:
                    printer.print("++");
                    break;
                case posDecrement:
                    printer.print("--");
                    break;
                default:
            }
        }

        @Override public void visit(final ConstructorDeclaration n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printJavadoc(n.getJavaDoc(), arg);
            printModifiers(n.getModifiers());

            printTypeParameters(n.getTypeParameters(), arg);
            if (!n.getTypeParameters().isEmpty()) {
                printer.print(" ");
            }
            printer.print(n.getName());

            printer.print("(");
            if (!n.getParameters().isEmpty()) {
                for (final Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
                    final Parameter p = i.next();
                    p.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
            }
            printer.print(")");

            if (!isNullOrEmpty(n.getThrows())) {
                printer.print(" throws ");
                for (final Iterator<NameExpr> i = n.getThrows().iterator(); i.hasNext();) {
                    final NameExpr name = i.next();
                    name.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
            }
            printer.print(" ");
            n.getBlock().accept(this, arg);
        }

        @Override public void visit(final MethodDeclaration n, final Object arg) {
            printOrphanCommentsBeforeThisChildNode(n);

            printJavaComment(n.getComment(), arg);
            printJavadoc(n.getJavaDoc(), arg);

            for (AnnotationExpr a: n.getAnnotations()) {
                if (a.getName().getName().equals("Test")) {
                    printer.printLn("#[test]");
                }
            }
            printModifiers(n.getModifiers());
            printer.print("fn ");
            if (n.isDefault()) {
                printer.print("default ");
            }
            printTypeParameters(n.getTypeParameters(), arg);
            if (!isNullOrEmpty(n.getTypeParameters())) {
                printer.print(" ");
            }

            int mark = printer.push();
            n.getType().accept(this, arg);
            String typeString = printer.getMark(mark);
            printer.pop();
            printer.print(" ");
            printer.print(toSnakeIfNecessary(n.getName()));

            printer.print("(");
            if (!ModifierSet.isStatic(n.getModifiers())) {
                printer.print("&self");
                if (!isNullOrEmpty(n.getParameters()))
                    printer.print(", ");
            }
            if (!isNullOrEmpty(n.getParameters())) {
                for (final Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
                    final Parameter p = i.next();
                    p.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
            }
            printer.print(") -> ");


            if (n.getArrayCount() > 0) {
                printer.print("/* ");
                for (int i = 0; i < n.getArrayCount(); i++) {
                    printer.print("[]");
                }
                printer.print(" */");
            }

            if (!isNullOrEmpty(n.getThrows())) {
                printer.print("/* ");
                printer.print(" throws ");
                for (final Iterator<ReferenceType> i = n.getThrows().iterator(); i.hasNext();) {
                    final ReferenceType name = i.next();
                    name.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
                printer.print(" */");
                printer.print("Result<");
                printer.print(typeString);
                printer.print("> ");
            } else {
                printer.print(typeString);
            }
            printer.print(" ");
            if (n.getBody() == null) {
                printer.print(";");
            } else {
                printer.print(" ");
                n.getBody().accept(this, arg);
            }
        }

        @Override public void visit(final Parameter n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printModifiers(n.getModifiers());
            if (n.getType() != null) {
                n.getType().accept(this, arg);
            }
            if (n.isVarArgs()) {
                printer.print("...");
            }
            printer.print(" ");
            n.getId().accept(this, arg);
        }

        @Override public void visit(MultiTypeParameter n, Object arg) {
            printModifiers(n.getModifiers());

            Type type = n.getType();
            if (type != null) {
                type.accept(this, arg);
            }

            printer.print(" ");
            n.getId().accept(this, arg);
        }

        @Override public void visit(final ExplicitConstructorInvocationStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            if (n.isThis()) {
                printTypeArgs(n.getTypeArgs(), arg);
                printer.print("this");
            } else {
                if (n.getExpr() != null) {
                    n.getExpr().accept(this, arg);
                    printer.print(".");
                }
                printTypeArgs(n.getTypeArgs(), arg);
                printer.print("super");
            }
            printArguments(n.getArgs(), arg);
            printer.print(";");
        }

        @Override public void visit(final VariableDeclarationExpr n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printModifiers(n.getModifiers());

            printer.print(" ");

            for (final Iterator<VariableDeclarator> i = n.getVars().iterator(); i.hasNext();) {
                final VariableDeclarator v = i.next();
                v.accept(this, n.getType());
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        @Override public void visit(final TypeDeclarationStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            n.getTypeDeclaration().accept(this, arg);
        }

        @Override public void visit(final AssertStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("assert ");
            n.getCheck().accept(this, arg);
            if (n.getMessage() != null) {
                printer.print(" : ");
                n.getMessage().accept(this, arg);
            }
            printer.print(";");
        }

        @Override public void visit(final BlockStmt n, final Object arg) {
            printOrphanCommentsBeforeThisChildNode(n);
            printJavaComment(n.getComment(), arg);
            printer.printLn("{");
            if (n.getStmts() != null) {
                printer.indent();
                for (final Statement s : n.getStmts()) {
                    s.accept(this, arg);
                    printer.printLn();
                }
                printer.unindent();
            }
            printOrphanCommentsEnding(n);
            printer.print("}");

        }

        @Override public void visit(final LabeledStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(n.getLabel());
            printer.print(": ");
            n.getStmt().accept(this, arg);
        }

        @Override public void visit(final EmptyStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(";");
        }

        @Override public void visit(final ExpressionStmt n, final Object arg) {
            printOrphanCommentsBeforeThisChildNode(n);
            printJavaComment(n.getComment(), arg);
            n.getExpression().accept(this, arg);
            printer.print(";");
        }

        @Override public void visit(final SwitchStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("switch(");
            n.getSelector().accept(this, arg);
            printer.printLn(") {");
            if (n.getEntries() != null) {
                printer.indent();
                for (final SwitchEntryStmt e : n.getEntries()) {
                    e.accept(this, arg);
                }
                printer.unindent();
            }
            printer.print("}");

        }

        @Override public void visit(final SwitchEntryStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            if (n.getLabel() != null) {
                printer.print("case ");
                n.getLabel().accept(this, arg);
                printer.print(":");
            } else {
                printer.print("default:");
            }
            printer.printLn();
            printer.indent();
            if (n.getStmts() != null) {
                for (final Statement s : n.getStmts()) {
                    s.accept(this, arg);
                    printer.printLn();
                }
            }
            printer.unindent();
        }

        @Override public void visit(final BreakStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("break");
            if (n.getId() != null) {
                printer.print(" ");
                printer.print(n.getId());
            }
            printer.print(";");
        }

        @Override public void visit(final ReturnStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("return");
            if (n.getExpr() != null) {
                printer.print(" ");
                n.getExpr().accept(this, arg);
            }
            printer.print(";");
        }

        @Override public void visit(final EnumDeclaration n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printJavadoc(n.getJavaDoc(), arg);
            printModifiers(n.getModifiers());

            printer.print("enum ");
            printer.print(n.getName());

            if (!n.getImplements().isEmpty()) {
                printer.print(" implements ");
                for (final Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
                    final ClassOrInterfaceType c = i.next();
                    c.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
            }

            printer.printLn(" {");
            printer.indent();
            if (n.getEntries() != null) {
                printer.printLn();
                for (final Iterator<EnumConstantDeclaration> i = n.getEntries().iterator(); i.hasNext();) {
                    final EnumConstantDeclaration e = i.next();
                    e.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
            }
            if (!n.getMembers().isEmpty()) {
                printer.printLn(";");
                printMembers(n.getMembers(), arg);
            } else {
                if (!n.getEntries().isEmpty()) {
                    printer.printLn();
                }
            }
            printer.unindent();
            printer.print("}");
        }

        @Override public void visit(final EnumConstantDeclaration n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printJavadoc(n.getJavaDoc(), arg);
            printer.print(n.getName());

            if (n.getArgs() != null) {
                printArguments(n.getArgs(), arg);
            }

            if (!n.getClassBody().isEmpty()) {
                printer.printLn(" {");
                printer.indent();
                printMembers(n.getClassBody(), arg);
                printer.unindent();
                printer.printLn("}");
            }
        }

        @Override public void visit(final EmptyMemberDeclaration n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printJavadoc(n.getJavaDoc(), arg);
            printer.print(";");
        }

        @Override public void visit(final InitializerDeclaration n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printJavadoc(n.getJavaDoc(), arg);
            if (n.isStatic()) {
                printer.print("static ");
            }
            n.getBlock().accept(this, arg);
        }

        @Override public void visit(final IfStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("if (");
            n.getCondition().accept(this, arg);
            final boolean thenBlock = n.getThenStmt() instanceof BlockStmt;
            if (thenBlock) // block statement should start on the same line
                printer.print(") ");
            else {
                printer.printLn(")");
                printer.indent();
            }
            n.getThenStmt().accept(this, arg);
            if (!thenBlock)
                printer.unindent();
            if (n.getElseStmt() != null) {
                if (thenBlock)
                    printer.print(" ");
                else
                    printer.printLn();
                final boolean elseIf = n.getElseStmt() instanceof IfStmt;
                final boolean elseBlock = n.getElseStmt() instanceof BlockStmt;
                if (elseIf || elseBlock) // put chained if and start of block statement on a same level
                    printer.print("else ");
                else {
                    printer.printLn("else");
                    printer.indent();
                }
                n.getElseStmt().accept(this, arg);
                if (!(elseIf || elseBlock))
                    printer.unindent();
            }
        }

        @Override public void visit(final WhileStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("while (");
            n.getCondition().accept(this, arg);
            printer.print(") ");
            n.getBody().accept(this, arg);
        }

        @Override public void visit(final ContinueStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("continue");
            if (n.getId() != null) {
                printer.print(" ");
                printer.print(n.getId());
            }
            printer.print(";");
        }

        @Override public void visit(final DoStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("do ");
            n.getBody().accept(this, arg);
            printer.print(" while (");
            n.getCondition().accept(this, arg);
            printer.print(");");
        }

        @Override public void visit(final ForeachStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("for (");
            n.getVariable().accept(this, arg);
            printer.print(" : ");
            n.getIterable().accept(this, arg);
            printer.print(") ");
            n.getBody().accept(this, arg);
        }

        @Override public void visit(final ForStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("for (");
            if (n.getInit() != null) {
                for (final Iterator<Expression> i = n.getInit().iterator(); i.hasNext();) {
                    final Expression e = i.next();
                    e.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
            }
            printer.print("; ");
            if (n.getCompare() != null) {
                n.getCompare().accept(this, arg);
            }
            printer.print("; ");
            if (n.getUpdate() != null) {
                for (final Iterator<Expression> i = n.getUpdate().iterator(); i.hasNext();) {
                    final Expression e = i.next();
                    e.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
            }
            printer.print(") ");
            n.getBody().accept(this, arg);
        }

        @Override public void visit(final ThrowStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("throw ");
            n.getExpr().accept(this, arg);
            printer.print(";");
        }

        @Override public void visit(final SynchronizedStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("synchronized (");
            n.getExpr().accept(this, arg);
            printer.print(") ");
            n.getBlock().accept(this, arg);
        }

        @Override public void visit(final TryStmt n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print("try ");
            if (!n.getResources().isEmpty()) {
                printer.print("(");
                Iterator<VariableDeclarationExpr> resources = n.getResources().iterator();
                boolean first = true;
                while (resources.hasNext()) {
                    visit(resources.next(), arg);
                    if (resources.hasNext()) {
                        printer.print(";");
                        printer.printLn();
                        if (first) {
                            printer.indent();
                        }
                    }
                    first = false;
                }
                if (n.getResources().size() > 1) {
                    printer.unindent();
                }
                printer.print(") ");
            }
            n.getTryBlock().accept(this, arg);
            if (n.getCatchs() != null) {
                for (final CatchClause c : n.getCatchs()) {
                    c.accept(this, arg);
                }
            }
            if (n.getFinallyBlock() != null) {
                printer.print(" finally ");
                n.getFinallyBlock().accept(this, arg);
            }
        }

        @Override public void visit(final CatchClause n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(" catch (");
            n.getParam().accept(this, arg);
            printer.print(") ");
            n.getCatchBlock().accept(this, arg);

        }

        @Override public void visit(final MemberValuePair n, final Object arg) {
            printJavaComment(n.getComment(), arg);
            printer.print(n.getName());
            printer.print(" = ");
            n.getValue().accept(this, arg);
        }

        @Override public void visit(final LineComment n, final Object arg) {
            if (!this.printComments) {
                return;
            }
            printer.print("//");
            String tmp = n.getContent();
            tmp = tmp.replace('\r', ' ');
            tmp = tmp.replace('\n', ' ');
            printer.printLn(tmp);
        }

        @Override public void visit(final BlockComment n, final Object arg) {
            if (!this.printComments) {
                return;
            }
            printer.print("/*");
            printer.print(n.getContent());
            printer.printLn("*/");
        }

        @Override
        public void visit(LambdaExpr n, Object arg) {
            printJavaComment(n.getComment(), arg);

            List<Parameter> parameters = n.getParameters();
            boolean printPar = false;
            printPar = n.isParametersEnclosed();

            if (printPar) {
                printer.print("(");
            }
            if (parameters != null) {
                for (Iterator<Parameter> i = parameters.iterator(); i.hasNext();) {
                    Parameter p = i.next();
                    p.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
            }
            if (printPar) {
                printer.print(")");
            }

            printer.print(" -> ");
            Statement body = n.getBody();
            if (body instanceof ExpressionStmt) {
                // Print the expression directly
                ((ExpressionStmt) body).getExpression().accept(this, arg);
            } else {
                body.accept(this, arg);
            }
        }


        @Override
        public void visit(MethodReferenceExpr n, Object arg) {
            printJavaComment(n.getComment(), arg);
            Expression scope = n.getScope();
            String identifier = n.getIdentifier();
            if (scope != null) {
                n.getScope().accept(this, arg);
            }

            printer.print("::");
            if (!n.getTypeParameters().isEmpty()) {
                printer.print("<");
                for (Iterator<TypeParameter> i = n.getTypeParameters().iterator(); i
                        .hasNext();) {
                    TypeParameter p = i.next();
                    p.accept(this, arg);
                    if (i.hasNext()) {
                        printer.print(", ");
                    }
                }
                printer.print(">");
            }
            if (identifier != null) {
                printer.print(identifier);
            }

        }

        @Override
        public void visit(TypeExpr n, Object arg) {
            printJavaComment(n.getComment(), arg);
            if (n.getType() != null) {
                n.getType().accept(this, arg);
            }
        }

        protected void printOrphanCommentsBeforeThisChildNode(final Node node){
            if (node instanceof Comment) return;

            Node parent = node.getParentNode();
            if (parent==null) return;
            List<Node> everything = new LinkedList<Node>();
            everything.addAll(parent.getChildrenNodes());
            sortByBeginPosition(everything);
            int positionOfTheChild = -1;
            for (int i=0;i<everything.size();i++){
                if (everything.get(i)==node) positionOfTheChild=i;
            }
            if (positionOfTheChild==-1) throw new RuntimeException("My index not found!!! "+node);
            int positionOfPreviousChild = -1;
            for (int i=positionOfTheChild-1;i>=0 && positionOfPreviousChild==-1;i--){
                if (!(everything.get(i) instanceof Comment)) positionOfPreviousChild = i;
            }
            for (int i=positionOfPreviousChild+1;i<positionOfTheChild;i++){
                Node nodeToPrint = everything.get(i);
                if (!(nodeToPrint instanceof Comment)) throw new RuntimeException("Expected comment, instead "+nodeToPrint.getClass()+". Position of previous child: "+positionOfPreviousChild+", position of child "+positionOfTheChild);
                nodeToPrint.accept(this,null);
            }
        }


        private void printOrphanCommentsEnding(final Node node){
            List<Node> everything = new LinkedList<Node>();
            everything.addAll(node.getChildrenNodes());
            sortByBeginPosition(everything);
            if (everything.isEmpty()) {
                return;
            }

            int commentsAtEnd = 0;
            boolean findingComments = true;
            while (findingComments && commentsAtEnd<everything.size()){
                Node last = everything.get(everything.size() - 1 - commentsAtEnd);
                findingComments = (last instanceof Comment);
                if (findingComments) {
                    commentsAtEnd++;
                }
            }
            for (int i=0; i<commentsAtEnd; i++){
                everything.get(everything.size()-commentsAtEnd+i).accept(this, null);
            }
        }


    }