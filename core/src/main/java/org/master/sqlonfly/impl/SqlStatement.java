/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.impl;

import java.sql.Blob;
import org.master.sqlonfly.core.SQLEngine;
import org.master.sqlonfly.interfaces.ISQLBatch;
import org.master.sqlonfly.interfaces.ISQLDataParams;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author RogovA
 */
public class SqlStatement {

    public static final String PARAM_SQL_CONNECTION = "@connection";
    public static final String PARAM_SQL_DIALECT = "@dialect";
    public static final String VAR_START = "<#";
    public static final String VAR_END = "#>";
    private final HashMap<String, SqlParam> params = new HashMap<String, SqlParam>();
    private final String sql;
    private final String name;
    private PreparedStatement statement;
    private boolean compiled = false;
    private boolean hasResults = false;
    private SQLWarning warning;

    public SqlStatement(String name, String sql) {
        this.name = name;
        this.sql = sql;
    }

    public String getName() {
        return name;
    }

    public PreparedStatement getSQLStatement() {
        return statement;
    }

    public void setParameter(String name, Object value, ISQLBatch.DataTypes type, int length, int scale) {
        SqlParam param = params.get(name);
        if (param == null) {
            param = new SqlParam(name, value, type.getSqlType(), scale);
            params.put(name, param);
        } else {
            param.value = value;
        }
    }

    public void setDynamicCode(String name, Object value) {
        SqlParam param = params.get(name);
        if (param == null) {
            param = new SqlParam(name, value, java.sql.Types.OTHER, 0);
            params.put(name, param);
        } else {
            param.value = value;
        }
    }

    public void compile(java.sql.Connection connection, boolean call) throws SQLException {
        compile(connection, call, true);
    }

    public void compile(java.sql.Connection connection, boolean call, boolean identity) throws SQLException {
        if (!compiled) {
            Pattern patt = Pattern.compile(VAR_START + "([^" + VAR_END.substring(0, 1) + "]*)" + VAR_END);

            Matcher match = patt.matcher(sql);
            boolean result = match.find();
            String query;

            if (result) {
                int i = 1;
                StringBuffer buf = new StringBuffer();
                do {
                    boolean output = false;
                    String paramName = match.group(1);
                    if (paramName.endsWith("|OUT")) {
                        output = true;
                        paramName = paramName.substring(0, paramName.length() - 4);
                    }

                    SqlParam param = params.get(paramName);
                    if (param == null) {
                        throw new SQLException("Parameter '" + paramName + "' not set");
                    }

                    if (param.type == java.sql.Types.OTHER) {
                        match.appendReplacement(buf, param.value == null ? "" : param.value.toString());
                    } else {
                        param.indexes.put(i++, output);
                        match.appendReplacement(buf, "\\?");
                    }
                    result = match.find();
                } while (result);
                match.appendTail(buf);
                query = buf.toString();
            } else {
                query = sql;
            }

            if (call) {
                statement = connection.prepareCall("{call " + query + "}");
            } else {
                if (identity) {
                    statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                } else {
                    statement = connection.prepareStatement(query);
                }
            }
            compiled = true;
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    public void close() {
        if (statement != null) {
            try {
                statement.close();
            } catch (Throwable ignore) {

            }
            statement = null;
        }
    }

    private void fillParams() throws SQLException {
        for (SqlParam param : params.values()) {
            for (Map.Entry<Integer, Boolean> entry : param.indexes.entrySet()) {
                Object value = param.value;

                if (value != null) {
                    if (param.type == java.sql.Types.TIMESTAMP && !(value instanceof java.sql.Timestamp) && value instanceof java.util.Date) {
                        value = new java.sql.Timestamp(((java.util.Date) value).getTime());
                    } else if (param.type == java.sql.Types.DATE && !(value instanceof java.sql.Date) && value instanceof java.util.Date) {
                        value = new java.sql.Date(((java.util.Date) value).getTime());
                    } else if (param.type == java.sql.Types.TIME && !(value instanceof java.sql.Time) && value instanceof java.util.Date) {
                        value = new java.sql.Time(((java.util.Date) value).getTime());
                    } else if (param.type == java.sql.Types.CLOB) {
                        Clob clob = statement.getConnection().createClob();
                        clob.setString(1, "" + value);
                        value = clob;
                    } else if (param.type == java.sql.Types.BLOB) {
                        Blob blob = statement.getConnection().createBlob();
                        blob.setBytes(1, (byte[]) value);
                        value = blob;
                    }
                }

                try {
                    if (param.scale == 0) {
                        statement.setObject(entry.getKey(), value, param.type);
                    } else {
                        statement.setObject(entry.getKey(), value, param.type, param.scale);
                    }
                } catch (SQLException e) {
                    throw new SQLException("Parameter '" + param.name + "' can't be filled with '" + value + "'", e);
                }

                if (entry.getValue() && statement instanceof CallableStatement) {
                    if (param.scale == 0) {
                        ((CallableStatement) statement).registerOutParameter(entry.getKey(), param.type);
                    } else {
                        ((CallableStatement) statement).registerOutParameter(entry.getKey(), param.type, param.scale);
                    }
                }
            }
        }
    }

    public void extract(ISQLDataParams result) throws SQLException {
        for (SqlParam param : params.values()) {
            try {
                for (Map.Entry<Integer, Boolean> entry : param.indexes.entrySet()) {
                    if (entry.getValue() && statement instanceof CallableStatement) {
                        Object value = ((CallableStatement) statement).getObject(entry.getKey());
                        if (value != null) {
                            result.putParameter(param.name, param.type, value);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new SQLException("Parameter '" + param.name + "' can't be extracted from sql", e);
            }
        }
    }

    public java.sql.ResultSet nextResult() throws SQLException {
        int rowsAffected;
        do {
            if (hasResults) {
                statement.clearWarnings();
                java.sql.ResultSet rs = statement.getResultSet();
                if (warning == null) {
                    warning = statement.getWarnings();
                } else {
                    warning.setNextWarning(statement.getWarnings());
                }
                hasResults = false;
                return rs;
            } else {
                rowsAffected = statement.getUpdateCount();
                hasResults = statement.getMoreResults();
            }
        } while (hasResults || rowsAffected != -1);

        return null;
    }

    public String getDebugSQL(SQLEngine mapper) {
        Pattern patt = Pattern.compile(VAR_START + "([^" + VAR_END.substring(0, 1) + "]*)" + VAR_END);

        Matcher match = patt.matcher(sql);
        boolean result = match.find();
        String query;

        if (result) {
            int i = 1;
            StringBuffer buf = new StringBuffer();
            do {
                boolean output = false;
                String paramName = match.group(1);
                if (paramName.endsWith("|OUT")) {
                    output = true;
                    paramName = paramName.substring(0, paramName.length() - 4);
                }

                SqlParam param = params.get(paramName);
                String replacement;
                if (param != null && param.value != null) {
                    try {
                        replacement = mapper.convertTo(param.value, String.class);
                    } catch (SQLException e) {
                        replacement = "" + param.value;
                    }

                    if (param.value instanceof String || param.value instanceof java.util.Date) {
                        replacement = "'" + replacement + "'";
                    }
                } else {
                    replacement = "null";
                }

                if (output) {
                    replacement = replacement + " out";
                }

                replacement = replacement.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\$");

                match.appendReplacement(buf, replacement);
                result = match.find();
            } while (result);
            match.appendTail(buf);
            query = buf.toString();
        } else {
            query = sql;
        }
        return query;
    }

    public boolean select() throws SQLException {
        fillParams();
        hasResults = statement.execute();
        return hasResults;
    }

    public int update() throws SQLException {
        fillParams();
        hasResults = false;
        return statement.executeUpdate();
    }

    public SQLWarning getWarnings() {
        try {
            return warning;
        } catch (Throwable t) {
            return null;
        }
    }

    private class SqlParam {

        private final String name;
        private Object value;
        private final int type;
        private final int scale;
        private final HashMap<Integer, Boolean> indexes = new HashMap<Integer, Boolean>();

        public SqlParam(String name, Object value, int type, int scale) {
            this.name = name;
            this.value = value;
            this.type = type;
            this.scale = scale;
        }

    }

}
