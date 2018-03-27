package org.master.sqlonfly.impl.scanner;

import com.sun.javadoc.*;
import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;
import org.master.sqlonfly.interfaces.ISQLBatch;

/**
 * Hello world!
 *
 */
public class SqlDoclet {

    private static String GenPath;

    private static String readConstant(String token, StringTokenizer tokens) {
        StringBuilder builder = new StringBuilder();
        token = token.substring(1);
        while (!token.endsWith("\"")) {
            builder.append(token);
            token = tokens.nextToken();
        }
        token = token.substring(0, token.length() - 1);
        builder.append(token);
        return builder.toString();
    }

    private static SqlMethodParam parseParam(String methodName, String text, ClassDoc clazz, String javaClass, boolean constAllowed) throws Exception {
        text = text.replaceAll("\\n", " ");
        StringTokenizer tokens = new StringTokenizer(text, " (),");
        String sqlConst = null;

        String paramName = "";
        if (tokens.hasMoreTokens()) {
            paramName = tokens.nextToken();
        }

        ISQLBatch.DataTypes sqlType = ISQLBatch.DataTypes.VARCHAR;
        if (tokens.hasMoreTokens()) {
            String sqlTypeName = tokens.nextToken();
            try {
                if ("DATETIME".equals(sqlTypeName)) {
                    sqlTypeName = "TIMESTAMP";
                }
                sqlType = ISQLBatch.DataTypes.valueOf(sqlTypeName);
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid SQL parameter type '" + sqlTypeName + "' for " + methodName + " parameter '" + paramName + "' ", e);
            }
        }

        String sqlLength = "0";
        if (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if (token.startsWith("\"")) {
                if (constAllowed) {
                    sqlConst = readConstant(token, tokens);
                    constAllowed = false;
                } else {
                    throw new Exception("Constant is not allowed in method " + methodName + " parameter " + paramName);
                }
            } else {
                sqlLength = token;
            }
        }

        String sqlScale = "0";
        if (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if (token.startsWith("\"")) {
                if (constAllowed) {
                    sqlConst = readConstant(token, tokens);
                    constAllowed = false;
                } else {
                    throw new Exception("Constant is not allowed in method " + methodName + " parameter " + paramName);
                }
            } else {
                sqlScale = token;
            }
        }

        if (constAllowed && tokens.hasMoreTokens()) {
            sqlConst = tokens.nextToken();
            if (sqlConst.startsWith("\"")) {
                sqlConst = sqlConst.substring(1, sqlConst.length() - 1);
                //constAllowed = false;
            } else {
                throw new Exception("Constant invalid format in method " + methodName + " parameter " + paramName);
            }
        }

        SqlMethodParam methodParam = new SqlMethodParam(
                paramName,
                sqlType,
                clazz,
                javaClass == null ? sqlType.getJavaClass().getName() : javaClass,
                sqlLength,
                sqlScale,
                sqlConst
        );

        return methodParam;
    }

    public static boolean start(RootDoc root) throws Exception {

        for (ClassDoc clazz : root.classes()) {

            boolean sqlClass = false;
            ClassDoc[] interfaces = clazz.interfaces();
            for (ClassDoc iface : interfaces) {
                if ("org.master.sqlonfly.interfaces.ISQLBatch".equals(iface.qualifiedName())) {
                    sqlClass = true;
                    break;
                }
            }

            if (sqlClass) {
                String connection = null;
                for (Tag paramTag : clazz.tags("@connection")) {
                    connection = paramTag.text();
                }

                ArrayList<SqlMethodParam> globalParams = new ArrayList<SqlMethodParam>();
                for (Tag paramTag : clazz.tags("@const")) {
                    globalParams.add(parseParam(clazz.name() + ".<global>", paramTag.text(), null, null, true));
                }

                if (connection == null) {
                    throw new Exception("No sql connection defined for class: " + clazz.name() + "; Use tag @connection in class javadoc.");
                }

                HashSet<String> generated = new HashSet<String>();
                HashSet<String> imports = new HashSet<String>();
                StringBuilder body = new StringBuilder();
                TextShift shift = new TextShift(4);

                body.append("/*\n");
                body.append(" * Automatically generated class by SqlDocScanner v. 1.2\n");
                body.append(" */\n\n");

                body
                        .append(shift.get())
                        .append("public class ")
                        .append(clazz.name())
                        .append("Impl extends AbstractSQLBatch<")
                        .append(clazz.name())
                        .append("> implements ")
                        .append(clazz.name())
                        .append(" {\n\n");
                shift.startBlock();//class start

                //Constructor
                body.append(shift.get()).append("public ").append(clazz.name()).append("Impl() {\n");
                shift.startBlock();//method start
                body.append(shift.get()).append("super(\"").append(clazz.name()).append("\");\n");
                shift.endBlock();//method end
                body.append(shift.get()).append("}\n\n");

                //ConnectionName
                body.append(shift.get()).append("@Override\n");
                body.append(shift.get()).append("public String getConnectionName() {\n");
                shift.startBlock();//method start
                body.append(shift.get()).append("return \"").append(connection).append("\";\n");
                shift.endBlock();//method end
                body.append(shift.get()).append("}\n\n");

                for (MethodDoc method : clazz.methods()) {

                    if (method.getRawCommentText() == null || method.getRawCommentText().trim().isEmpty()) {
                        continue;
                    }

                    try {
                        String exception = null;
                        for (ClassDoc exdoc : method.thrownExceptions()) {
                            if (exception == null) {
                                exception = exdoc.qualifiedName();
                            } else {
                                exception = null;
                                break;
                            }
                        }

                        boolean debugged = method.tags("@debug").length > 0;

                        String execType = null;
                        for (Tag paramTag : method.tags("@execute")) {
                            execType = paramTag.text();
                        }

                        String returnType = null;
                        for (Tag paramTag : method.tags("@return")) {
                            returnType = paramTag.text();
                        }

                        String rowcount = null;
                        for (Tag paramTag : method.tags("@rowcount")) {
                            rowcount = paramTag.text();
                        }

                        ArrayList<SqlMethodParam> methodParams = new ArrayList<SqlMethodParam>();
                        methodParams.addAll(globalParams);

                        for (Tag paramTag : method.tags("@const")) {
                            methodParams.add(parseParam(clazz.name() + "." + method.name(), paramTag.text(), null, null, true));
                        }

                        Parameter[] params = method.parameters();
                        Tag[] paramTags = method.paramTags();
                        for (int i = 0; i < params.length; i++) {
                            imports.add(params[i].type().qualifiedTypeName());

                            methodParams.add(parseParam(clazz.name() + "." + method.name(), paramTags[i].text(), params[i].type().asClassDoc(), params[i].typeName(), false));
                        }

                        body
                                .append(shift.get())
                                .append("@Override\n");

                        body
                                .append(shift.get())
                                .append("public ")
                                .append(method.returnType().simpleTypeName())
                                .append("[]".equals(method.returnType().dimension()) ? "[] " : "");

                        body
                                .append(" ")
                                .append(method.name())
                                .append("(");

                        boolean firstParam = true;
                        for (SqlMethodParam methodParam : methodParams) {
                            if (methodParam.constValue == null) {
                                if (!firstParam) {
                                    body.append(", ");
                                }
                                body.append(methodParam.javaType);
                                body.append(" ");

                                body.append(methodParam.name);
                                firstParam = false;
                            }
                        }

                        if (exception == null || "java.sql.SQLException".equals(exception)) {
                            body.append(" ) throws java.sql.SQLException {\n");
                        } else {
                            body.append(" ) throws ").append(exception).append(" {\n");
                        }
                        shift.startBlock();//method start

                        boolean firstSee = true;
                        boolean hasSeeTags = false;
                        for (SeeTag seeTag : method.seeTags()) {
                            hasSeeTags = true;
                            if (firstSee) {
                                body.append(shift.get()).append("if (\"");
                                firstSee = false;
                            } else {
                                body.append(" else if (\"");
                            }
                            body.append(seeTag.label());
                            body.append("\".equals(getDialect())) {\n");
                            shift.startBlock();//if start

                            if (!"void".equals(method.returnType().typeName())) {
                                body.append(shift.get()).append("return ");
                            } else {
                                body.append(shift.get());
                            }

                            String seeMethod = seeTag.referencedMemberName();
                            body.append(seeMethod.substring(0, seeMethod.indexOf('(')));
                            body.append("(");
                            firstParam = true;
                            for (SqlMethodParam methodParam : methodParams) {
                                if (methodParam.constValue == null) {
                                    if (!firstParam) {
                                        body.append(", ");
                                    }
                                    body.append(methodParam.name);
                                    firstParam = false;
                                }
                            }
                            body.append(");\n");
                            shift.endBlock(); //if end
                            body.append("}");

                        }

                        if (hasSeeTags) {
                            body.append(" else {\n");
                            shift.startBlock();
                        }

                        if (exception != null && !"java.sql.SQLException".equals(exception)) {
                            body.append(shift.get()).append("try {\n");
                            shift.startBlock();
                        }

                        StringBuilder sqlbuf = new StringBuilder();
                        for (Tag codeTag : method.inlineTags()) {
                            if ("@code".equals(codeTag.name())) {
                                body.append(shift.get()).append("String query = \"");
                                shift.startBlock();//query shift x2 start
                                shift.startBlock();
                                StringTokenizer sqlTokens = new StringTokenizer(codeTag.text(), "\n");
                                boolean first = true;
                                while (sqlTokens.hasMoreTokens()) {
                                    String str = sqlTokens.nextToken();
                                    if (!str.isEmpty()) {
                                        if (first) {
                                            body.append(str);
                                            first = false;
                                        } else {
                                            body.append("\\n\"\n");
                                            body.append(shift.get());
                                            body.append("+ \"");
                                            body.append(str);
                                        }
                                        body.append(" ");
                                        sqlbuf.append(str);
                                    }
                                }
                                body.append("\\n\";\n\n");
                                shift.endBlock();//query shift x2 end
                                shift.endBlock();
                            }
                        }
                        String sql = sqlbuf.toString();
                        body.append("\n");

                        body
                                .append(shift.get())
                                .append("getMapper().beforeBatchExecute(this, \"")
                                .append(method.name())
                                .append("\");\n");
                        body.append(shift.get()).append("getMapper().debug(\"<SQL.").append(method.name()).append("> prepared\");\n\n");

                        body
                                .append(shift.get())
                                .append("try {\n\n");
                        shift.startBlock(); //try start

                        body
                                .append(shift.get())
                                .append("java.sql.Connection connection = getMapper().takeConnection(getConnectionName());\n");
                        body.append(shift.get()).append("getMapper().debug(\"<SQL.").append(method.name()).append("> connection received\");\n\n");

                        body
                                .append(shift.get())
                                .append("try {\n\n");
                        shift.startBlock(); //try start
                        body
                                .append(shift.get())
                                .append("SqlStatement statement = getMapper().prepareStatement(\"")
                                .append(method.name())
                                .append("\", query, connection);\n");
                        body.append(shift.get()).append("getMapper().debug(\"<SQL.").append(method.name()).append("> statement created\");\n\n");

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        for (SqlMethodParam methodParam : methodParams) {
                            String paramName = methodParam.name;
                            if (sql.contains("<#" + paramName + "#>") || sql.contains("<#" + paramName + "|OUT#>")) {

                                if (paramName.endsWith("|OUT")) {
                                    paramName = paramName.substring(0, paramName.length() - 4);
                                }

                                if (methodParam.sqlType == ISQLBatch.DataTypes.DYNAMIC) {
                                    body.append(shift.get());
                                    body.append("statement.setDynamicCode(\"");
                                    body.append(paramName);
                                    body.append("\", ");
                                    body.append(paramName);
                                    body.append(");\n");
                                } else {
                                    body.append(shift.get());
                                    body.append("statement.setParameter(\"");
                                    body.append(paramName);
                                    body.append("\", ");
                                    if (methodParam.constValue == null) {
                                        if (InstanceOf(methodParam.clazz, "java.lang.Enum")) {
                                            body.append("getMapper().prepareValue(").append(paramName).append(")");
                                        } else {
                                            body.append(paramName);
                                        }
                                    } else {
                                        switch (methodParam.sqlType) {
                                            case INT:
                                            case INTEGER:
                                            case DOUBLE:
                                            case FLOAT:
                                                body.append(methodParam.constValue);
                                                break;

                                            case BIGINT:
                                                body.append(methodParam.constValue);
                                                body.append("L");
                                                break;

                                            case DATE:
                                                body.append("new java.sql.Date(");
                                                body.append(GetConstTime(sdf, methodParam.constValue));
                                                body.append(")");
                                                break;

                                            case TIME:
                                                body.append("new java.sql.Time(");
                                                body.append(GetConstTime(sdf, methodParam.constValue));
                                                body.append(")");
                                                break;

                                            case TIMESTAMP:
                                                body.append("new java.sql.Timestamp(");
                                                body.append(GetConstTime(sdf, methodParam.constValue));
                                                body.append(")");
                                                break;

                                            case DECIMAL:
                                            case NUMERIC:
                                                body.append("new BigDecimal(\"");
                                                body.append(methodParam.constValue);
                                                body.append(", new MathContext(");
                                                body.append(methodParam.scale);
                                                body.append("))");
                                                break;

                                            default:
                                                body.append("\"");
                                                body.append(methodParam.constValue);
                                                body.append("\"");
                                        }
                                    }
                                    body.append(", ISQLBatch.DataTypes.");
                                    body.append(methodParam.sqlType.name());
                                    body.append(", ");
                                    body.append(methodParam.length);
                                    body.append(", ");
                                    body.append(methodParam.scale);
                                    body.append(");\n");
                                }
                            }
                        }
                        body.append("\n");
                        body.append(shift.get()).append("getMapper().debug(\"<SQL.").append(method.name()).append("> parameters prepared\");\n\n");

                        body.append(shift.get()).append("try {\n");
                        shift.startBlock();
                        body.append(shift.get()).append("statement.compile(connection, ").append("call".equals(execType) ? "true" : "false").append(");\n");
                        body.append(shift.get()).append("getMapper().debug(\"<SQL.").append(method.name()).append("> statement compiled\");\n\n");

                        if (debugged) {
                            body.append(shift.get()).append("getMapper().debug(this, statement, statement.getDebugSQL(getMapper()));\n\n");
                        }

                        if (rowcount == null) {
                            body
                                    .append(shift.get())
                                    .append("if (getRowcount() != null) {\n");
                            shift.startBlock(); //if start

                            body
                                    .append(shift.get())
                                    .append("getMapper().setRowcount(statement, getRowcount());\n");

                            shift.endBlock(); //if end
                            body
                                    .append(shift.get())
                                    .append("}\n\n");
                        } else {
                            body
                                    .append(shift.get())
                                    .append("getMapper().setRowcount(statement, ").append(rowcount).append(");\n\n");
                        }

                        body.append(shift.get());
                        if ("update".equals(execType)) {
                            body.append("int ra = statement.update();\n\n");
                        } else if ("select".equals(execType)) {
                            body.append("boolean selected = statement.select();\n\n");
                        } else if ("call".equals(execType)) {
                            body.append("boolean selected = statement.select();\n\n");
                        } else {
                            throw new Exception("Invalid execute type '" + execType + "' for method " + clazz.name() + "." + method.name() + "(). Use 'select', 'call' or 'update' instead.");
                        }
                        body.append("\n");
                        body.append(shift.get()).append("getMapper().debug(\"<SQL.").append(method.name()).append("> statement executed\");\n\n");

                        body.append(shift.get()).append("setWarnings(statement.getWarnings());\n");

                        boolean directTable = false;
                        if (!"void".equals(method.returnType().qualifiedTypeName())) {
                            body.append(shift.get());

                            if ("@@identity".equals(returnType)) {
                                body.append("return getMapper().getIdentity(statement);\n\n");
                            } else if ("@@custom".equals(returnType) && InstanceOf(method.returnType().asClassDoc(), "org.master.sqlonfly.interfaces.ISQLDataTable")) {
                                body.append("return new ").append(method.returnType().simpleTypeName()).append("Impl(statement.nextResult(), getMapper());\n\n");
                            } else if ("@@custom".equals(returnType) && InstanceOf(method.returnType().asClassDoc(), "org.master.sqlonfly.interfaces.ISQLDataParams")) {
                                body.append("return new ").append(method.returnType().simpleTypeName()).append("Impl(statement, getMapper());\n\n");
                            } else if ("@@rowcount".equals(returnType)) {
                                if ("update".equals(execType)) {
                                    body.append("return ra;\n\n");
                                } else {
                                    throw new Exception("Invalid execute type '" + execType + "' for method " + clazz.name() + "." + method.name() + "() with return count of changing data. Use 'update' instead.");
                                }
                            } else if (InstanceOf(method.returnType().asClassDoc(), "org.master.sqlonfly.interfaces.ISQLDataParams")) {
                                body.append("return (").append(method.returnType().simpleTypeName()).append(") createParams(statement, ").append(method.returnType().simpleTypeName()).append(".class, null);\n");
                            } else if (InstanceOf(method.returnType().asClassDoc(), "org.master.sqlonfly.interfaces.ISQLDirectTable")) {
                                directTable = true;
                                body.append(method.returnType().simpleTypeName());
                                body.append(" directTable = (");
                                body.append(method.returnType().simpleTypeName());
                                body.append(") getMapper().createTable(statement.nextResult(), ");
                                body.append(method.returnType().simpleTypeName());
                                body.append(".class);\n");
                                body.append(shift.get());
                                body.append("directTable.init(statement, connection);\n");
                                body.append("return directTable;\n");
                            } else if (InstanceOf(method.returnType().asClassDoc(), "org.master.sqlonfly.interfaces.ISQLDataTable")) {
                                if ("select".equals(execType) || "call".equals(execType)) {
                                    if ("[]".equals(method.returnType().dimension())) {
                                        body.append("ArrayList<").append(method.returnType().simpleTypeName()).append("> list = new ArrayList<").append(method.returnType().simpleTypeName()).append(">();\n");
                                        body.append(shift.get()).append("java.sql.ResultSet rs = statement.nextResult();\n");
                                        body.append(shift.get()).append("while (rs != null) {\n");
                                        shift.startBlock(); //while start
                                        body.append(shift.get()).append("list.add((").append(method.returnType().simpleTypeName()).append(") createTable(statement.nextResult(), ").append(method.returnType().simpleTypeName()).append(".class));\n");
                                        shift.endBlock(); //while end
                                        body.append(shift.get());
                                        body.append("}\n");
                                        body.append(shift.get());
                                        body.append("return list.toArray(new ").append(method.returnType().simpleTypeName()).append("[list.size()]);\n");
                                    } else {
                                        body.append("return (");
                                        body.append(method.returnType().simpleTypeName());
                                        body.append(") getMapper().createTable(statement.nextResult(), ");
                                        body.append(method.returnType().simpleTypeName());
                                        body.append(".class);\n");
                                    }
                                } else {
                                    throw new Exception("Invalid execute type '" + execType + "' for method " + clazz.name() + "." + method.name() + "() with fetching table data. Use 'select' instead.");
                                }
                            } else if (InstanceOf(method.returnType().asClassDoc(), "org.master.sqlonfly.interfaces.ISQLDataRow")) {
                                if ("select".equals(execType) || "call".equals(execType)) {
                                    body.append("return (");
                                    body.append(method.returnType().simpleTypeName());
                                    body.append(") getMapper().createRow(statement.nextResult(), ");
                                    body.append(method.returnType().simpleTypeName());
                                    body.append(".class);\n");
                                } else {
                                    throw new Exception("Invalid execute type '" + execType + "' for method " + clazz.name() + "." + method.name() + "() with fetching row data. Use 'select' instead.");
                                }
                            } else {
                                if ("select".equals(execType) || "call".equals(execType)) {
                                    body.append("if (selected) {\n");
                                    shift.startBlock(); //if start
                                    body.append(shift.get());
                                    body.append("java.sql.ResultSet rs = statement.nextResult();\n");

                                    if ("[]".equals(method.returnType().dimension())) {
                                        body.append(shift.get());
                                        body.append("return (");
                                        body.append(method.returnType().qualifiedTypeName()).append(method.returnType().dimension());
                                        body.append(") getMapper().createArray(rs, ");
                                        if (returnType != null && !returnType.trim().isEmpty()) {
                                            body.append("\"").append(returnType).append("\", ");
                                        } else {
                                            body.append("\"\", ");
                                        }
                                        body.append(method.returnType().qualifiedTypeName());
                                        body.append(".class);\n");
                                    } else {
                                        body.append(shift.get());
                                        body.append("if (rs.next()) {\n");
                                        shift.startBlock(); //if start

                                        body.append(shift.get());
                                        body.append("return (");
                                        body.append(method.returnType().qualifiedTypeName());
                                        body.append(") getMapper().returnValue(rs, ");
                                        if (returnType != null && !returnType.trim().isEmpty()) {
                                            body.append("\"").append(returnType).append("\", ");
                                        } else {
                                            body.append("\"\", ");
                                        }
                                        body.append(method.returnType().qualifiedTypeName());
                                        body.append(".class);\n");

                                        shift.endBlock(); //if end
                                        body.append(shift.get());
                                        body.append("}\n");
                                    }

                                    shift.endBlock(); //if end
                                    body.append(shift.get());
                                    body.append("}\n");
                                    body.append(shift.get());
                                    body.append("return null;\n");
                                } else {
                                    throw new Exception("Invalid execute type '" + execType + "' for method " + clazz.name() + "." + method.name() + "() with fetching field data. Use 'select' instead.");
                                }
                            }
                        } else {
                            body.append(shift.get()).append("while (statement.nextResult() != null) {\n");
                            body.append(shift.get()).append("}\n");
                        }

                        if (directTable) {
                            shift.endBlock();//try end
                            body.append(shift.get()).append("} catch (Throwable t) {\n");
                            shift.startBlock();//catch start
                            body.append(shift.get()).append("getMapper().releaseStatement(statement);\n");
                            body.append(shift.get()).append("throw t;\n");
                            shift.endBlock();//catch end
                            body.append(shift.get()).append("}\n");
                        } else {
                            shift.endBlock();//try end
                            body.append(shift.get()).append("} finally {\n");
                            shift.startBlock();//finally start
                            body.append(shift.get()).append("getMapper().releaseStatement(statement);\n");
                            body.append(shift.get()).append("getMapper().debug(\"<SQL.").append(method.name()).append("> result fetched\");\n\n");
                            shift.endBlock();//finally end
                            body.append(shift.get()).append("}\n");
                        }

                        if (directTable) {
                            shift.endBlock();//try end
                            body.append(shift.get()).append("} catch (Throwable t) {\n");
                            shift.startBlock();//catch start
                            body.append(shift.get()).append("getMapper().releaseConnection(connection);\n");
                            body.append(shift.get()).append("throw t;\n");
                            shift.endBlock();//catch end
                            body.append(shift.get()).append("}\n");
                        } else {
                            shift.endBlock();//try end
                            body.append(shift.get()).append("} finally {\n");
                            shift.startBlock();//finally start
                            body.append(shift.get()).append("getMapper().releaseConnection(connection);\n");
                            shift.endBlock();//finally end
                            body.append(shift.get()).append("}\n");
                        }

                        shift.endBlock();//try end
                        body.append(shift.get());
                        body.append("} finally {\n");
                        shift.startBlock();//finally start

                        body.append(shift.get()).append("getMapper().afterBatchExecute(this, \"");
                        body.append(method.name());
                        body.append("\");\n\n");
                        body.append(shift.get()).append("getMapper().debug(\"<SQL.").append(method.name()).append("> execution completed\");\n\n");

                        shift.endBlock();//finally end
                        body.append(shift.get());
                        body.append("}\n");

                        if (exception != null && !"java.sql.SQLException".equals(exception)) {
                            shift.endBlock();//try end
                            body.append(shift.get());
                            body.append("} catch (java.sql.SQLException e) {\n");
                            shift.startBlock();//catch start
                            body.append(shift.get());
                            body.append("throw new ").append(exception).append("(e);\n");
                            shift.endBlock();//catch end
                            body.append(shift.get());
                            body.append("}\n");
                        }

                        if (hasSeeTags) {
                            shift.endBlock(); //if end
                            body.append(shift.get());
                            body.append("}\n\n");
                        }

                        shift.endBlock(); //method end
                        body.append(shift.get());
                        body.append("}\n\n");

                        if ("@@custom".equals(returnType) && !generated.contains(method.returnType().simpleTypeName())) {

                            if (InstanceOf(method.returnType().asClassDoc(), "org.master.sqlonfly.interfaces.ISQLDataTable")) {
                                body.append(shift.get());
                                body.append("private static class ").append(method.returnType().simpleTypeName()).append("Impl extends ").append(method.returnType().simpleTypeName()).append(" {\n\n");
                                shift.startBlock();//class start

                                body.append(shift.get());
                                body.append("public ").append(method.returnType().simpleTypeName()).append("Impl (java.sql.ResultSet rs, SqlMapper mapper) throws ");

                                if (exception == null || "java.sql.SQLException".equals(exception)) {
                                    body.append(shift.get()).append("java.sql.SQLException {\n");
                                    shift.startBlock();//constructor start
                                } else {
                                    body.append(shift.get()).append(exception).append(" {\n");
                                    shift.startBlock();//constructor start
                                    body.append(shift.get());
                                    body.append("try {\n");
                                    shift.startBlock();//try start
                                }
                                body.append(shift.get());
                                body.append("setMapper(mapper);\n");
                                body.append(shift.get());
                                body.append("reload(rs);\n");

                                if (exception != null && !"java.sql.SQLException".equals(exception)) {
                                    shift.endBlock();//try end
                                    body.append(shift.get()).append("} catch(java.sql.SQLException e) {\n");
                                    shift.startBlock();//catch start
                                    body.append(shift.get()).append("throw new ").append(exception).append("(e);\n");
                                    shift.endBlock();//catch end
                                    body.append(shift.get()).append("}\n");
                                }
                                shift.endBlock();//constructor end
                                body.append(shift.get());
                                body.append("}\n\n");

                                for (MethodDoc getter : method.returnType().asClassDoc().methods()) {
                                    imports.add(getter.returnType().qualifiedTypeName());

                                    String methodExch = null;
                                    for (ClassDoc exdoc : getter.thrownExceptions()) {
                                        if (methodExch == null) {
                                            methodExch = exdoc.qualifiedName();
                                        } else {
                                            methodExch = null;
                                            break;
                                        }
                                    }

                                    body.append(shift.get());
                                    body.append("@Override\n");
                                    body.append(shift.get());
                                    body.append("public ").append(getter.returnType().simpleTypeName()).append(" ").append(getter.name()).append("() throws ");
                                    if (methodExch == null || "java.sql.SQLException".equals(methodExch)) {
                                        body.append("java.sql.SQLException {\n");
                                        shift.startBlock();//method start
                                    } else {
                                        body.append(methodExch).append(" {\n");
                                        shift.startBlock();//method start
                                        body.append(shift.get());
                                        body.append("try {\n");
                                        shift.startBlock();//try start
                                    }

                                    body.append(shift.get());
                                    body.append("return getMapper().convertValue(getObject(\"").append(getter.name()).append("\"), ").append(getter.returnType().simpleTypeName()).append(".class);\n");
                                    if (methodExch != null && !"java.sql.SQLException".equals(methodExch)) {
                                        shift.endBlock();//try end
                                        body.append(shift.get());
                                        body.append("} catch(java.sql.SQLException e) {\n");
                                        shift.startBlock();//catch start
                                        body.append(shift.get());
                                        body.append("throw new ").append(methodExch).append("(e);\n");
                                        shift.endBlock();//catch end
                                        body.append(shift.get());
                                        body.append("}\n");
                                    }
                                    shift.endBlock();//method end
                                    body.append(shift.get());
                                    body.append("}\n\n");
                                }

                                shift.endBlock();//class end
                                body.append("}\n\n");
                            } else if (InstanceOf(method.returnType().asClassDoc(), "org.master.sqlonfly.interfaces.ISQLDataParams")) {
                                body.append(shift.get());
                                body.append("private static class ").append(method.returnType().simpleTypeName()).append("Impl extends ").append(method.returnType().simpleTypeName()).append(" {\n\n");
                                shift.startBlock();//class start

                                body.append(shift.get());
                                body.append("public ").append(method.returnType().simpleTypeName()).append("Impl (SqlStatement statement, SqlMapper mapper) throws ");

                                if (exception == null || "java.sql.SQLException".equals(exception)) {
                                    body.append(shift.get()).append("java.sql.SQLException {\n");
                                    shift.startBlock();//constructor start
                                } else {
                                    body.append(shift.get()).append(exception).append(" {\n");
                                    shift.startBlock();//constructor start
                                    body.append(shift.get());
                                    body.append("try {\n");
                                    shift.startBlock();//try start
                                }
                                body.append(shift.get()).append("setMapper(mapper);\n");
                                body.append(shift.get()).append("statement.extract(this);\n");
                                body.append(shift.get()).append("java.sql.ResultSet rs = statement.nextResult();\n");
                                body.append(shift.get()).append("while (rs != null) {\n");
                                shift.startBlock();//constructor start
                                body.append(shift.get()).append("append(mapper.createTable(rs, null));\n");
                                shift.endBlock();
                                body.append(shift.get()).append("}\n");

                                if (exception != null && !"java.sql.SQLException".equals(exception)) {
                                    shift.endBlock();//try end
                                    body.append(shift.get()).append("} catch(java.sql.SQLException e) {\n");
                                    shift.startBlock();//catch start
                                    body.append(shift.get()).append("throw new ").append(exception).append("(e);\n");
                                    shift.endBlock();//catch end
                                    body.append(shift.get()).append("}\n");
                                }
                                shift.endBlock();//constructor end
                                body.append(shift.get());
                                body.append("}\n\n");

                                for (MethodDoc getter : method.returnType().asClassDoc().methods()) {
                                    imports.add(getter.returnType().qualifiedTypeName());

                                    String methodExch = null;
                                    for (ClassDoc exdoc : getter.thrownExceptions()) {
                                        if (methodExch == null) {
                                            methodExch = exdoc.qualifiedName();
                                        } else {
                                            methodExch = null;
                                            break;
                                        }
                                    }

                                    body.append(shift.get());
                                    body.append("@Override\n");
                                    body.append(shift.get());
                                    body.append("public ").append(getter.returnType().simpleTypeName()).append(" ").append(getter.name()).append("() throws ");
                                    if (methodExch == null || "java.sql.SQLException".equals(methodExch)) {
                                        body.append("java.sql.SQLException {\n");
                                        shift.startBlock();//method start
                                    } else {
                                        body.append(methodExch).append(" {\n");
                                        shift.startBlock();//method start
                                        body.append(shift.get());
                                        body.append("try {\n");
                                        shift.startBlock();//try start
                                    }

                                    body.append(shift.get());
                                    body.append("return getMapper().convertValue(getObject(\"").append(getter.name()).append("\"), ").append(getter.returnType().simpleTypeName()).append(".class);\n");
                                    if (methodExch != null && !"java.sql.SQLException".equals(methodExch)) {
                                        shift.endBlock();//try end
                                        body.append(shift.get());
                                        body.append("} catch(java.sql.SQLException e) {\n");
                                        shift.startBlock();//catch start
                                        body.append(shift.get());
                                        body.append("throw new ").append(methodExch).append("(e);\n");
                                        shift.endBlock();//catch end
                                        body.append(shift.get());
                                        body.append("}\n");
                                    }
                                    shift.endBlock();//method end
                                    body.append(shift.get());
                                    body.append("}\n\n");
                                }

                                shift.endBlock();//class end
                                body.append("}\n\n");
                            }
                            generated.add(method.returnType().simpleTypeName());
                        }

                    } catch (Throwable t) {
                        throw new Exception("Method processing failed: " + method.qualifiedName(), t);
                    }
                }
                shift.endBlock();//class end
                body.append("}");

                StringBuilder total = new StringBuilder();

                total.append("package ");
                total.append(clazz.containingPackage().name());
                total.append(";\n\n");

                imports.add("org.master.sqlonfly.core.*");
                imports.add("org.master.sqlonfly.impl.*");
                imports.add("org.master.sqlonfly.interfaces.*");
                imports.add("java.math.BigDecimal");
                imports.add("java.util.ArrayList");

                for (String imp : imports) {
                    if (!imp.isEmpty() && !imp.startsWith("java.lang") && !"byte".equals(imp)) {
                        total.append("import ");
                        total.append(imp);
                        total.append(";\n");
                    }
                }
                total.append("\n");
                total.append(body);

                File path = new File(GenPath + File.separator + clazz.containingPackage().name().replaceAll("\\.", "\\" + File.separator));
                System.out.println("Generating sql wrapper for: " + clazz.name());
                path.mkdirs();
                File out = new File(path, clazz.name() + "Impl.java");
                out.delete();
                out.createNewFile();
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(total.toString().getBytes("UTF-8"));
                fos.close();
            }
        }

        return true;
    }

    private static String GetConstTime(SimpleDateFormat sdf, String constValue) throws ParseException {
        if ("NOW".equals(constValue)) {
            return "System.currentTimeMillis()";
        } else {
            return "" + sdf.parse(constValue).getTime();
        }
    }

    private static boolean InstanceOf(ClassDoc doc, String interfaceName) {
        if (doc == null) {
            return false;
        } else if (interfaceName.equals(doc.qualifiedTypeName())) {
            return true;
        }

        for (ClassDoc iface : doc.interfaces()) {
            if (InstanceOf(iface, interfaceName)) {
                return true;
            }
        }

        return InstanceOf(doc.superclass(), interfaceName);
    }

    public static int optionLength(String option) {
        if (option.equals("-genpath")) {
            return 2;
        }
        return 0;
    }

    public static boolean validOptions(String options[][], DocErrorReporter reporter) {
        for (String[] option : options) {
            if (option[0].equals("-genpath")) {
                GenPath = option[1];
            }
        }
        return true;

    }

    private static class SqlMethodParam {

        String name;
        ISQLBatch.DataTypes sqlType;
        ClassDoc clazz;
        String javaType;
        String length;
        String scale;
        String constValue;

        public SqlMethodParam(String name, ISQLBatch.DataTypes sqlType, ClassDoc clazz, String javaType, String length, String scale, String constValue) {
            this.name = name;
            this.sqlType = sqlType;
            this.javaType = javaType;
            this.length = length;
            this.scale = scale;
            this.constValue = constValue;
            this.clazz = clazz;
        }

    }

    private static class TextShift {

        private final StringBuilder shift = new StringBuilder();
        private final int spaces;

        public TextShift(int spaces) {
            this.spaces = spaces;
        }

        public void startBlock() {
            for (int i = 0; i < spaces; i++) {
                shift.append(" ");
            }
        }

        public void endBlock() {
            if (shift.length() <= spaces) {
                shift.setLength(0);
            } else {
                shift.setLength(shift.length() - spaces);
            }
        }

        public String get() {
            return shift.toString();
        }
    }
}
