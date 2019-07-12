/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.core;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import org.master.sqlonfly.impl.SqlDefaultDataParams;
import org.master.sqlonfly.impl.SqlDefaultDataRow;
import org.master.sqlonfly.impl.SqlDefaultDataTable;
import org.master.sqlonfly.interfaces.IConnectionProvider;
import org.master.sqlonfly.interfaces.ISQLBatch;
import org.master.sqlonfly.impl.SqlStatement;
import org.master.sqlonfly.interfaces.ISQLDataParams;
import org.master.sqlonfly.interfaces.ISQLDataRow;
import org.master.sqlonfly.interfaces.ISQLDataTable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.master.sqlonfly.interfaces.ILogic;
import org.master.sqlonfly.interfaces.IReturnableLogic;
import org.master.sqlonfly.interfaces.ISQLData;

/**
 *
 * @author RogovA
 */
public class SQLEngine {

    protected enum SqlState {

        OnDemand,
        InTransaction;
    }

    private final ThreadLocal<HashMap<String, java.sql.Connection>> connectionCache = new InheritableThreadLocal<HashMap<String, Connection>>();
    private final ThreadLocal<HashMap<String, SqlStatement>> statementCache = new InheritableThreadLocal<HashMap<String, SqlStatement>>();
    private final ThreadLocal<SqlState> state = new InheritableThreadLocal<SqlState>();
    private final IConnectionProvider provider;

    public SQLEngine(IConnectionProvider provider) {
        this.provider = provider;
    }

    public long getIdentity(SqlStatement statement) throws SQLException {
        ResultSet rs = null;
        try {
            rs = statement.getSQLStatement().getGeneratedKeys();
            long i = 0;
            if (rs.next()) {
                i = rs.getLong(1);
            }
            return i;
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
    }

    public void debug(String message) {
        provider.debug(message);
    }

    public void debug(ISQLBatch batch, SqlStatement statement, String query) {
        provider.debugQuery(batch.getName() + "." + statement.getName(), query);
    }

    public Class resolve(int sqlType) {
        return provider.resolve(ISQLBatch.DataTypes.valueOf(sqlType));
    }

    public void setRowcount(SqlStatement statement, int rowcount) throws SQLException {
        statement.getSQLStatement().setMaxRows(rowcount);
    }

    public void afterBatchExecute(ISQLBatch batch, String methodName) throws SQLException {
        try {
            provider.afterBatchExecute(batch, methodName);
        } catch (InterruptedException e) {
            throw new SQLException("Process has been interrupted", e);
        }
    }

    public void beforeBatchExecute(ISQLBatch batch, String methodName) {
        provider.beforeBatchExecute(batch, methodName);
    }

    public Object prepareValue(Object value) throws SQLException {
        try {
            return provider.prepareValue(value);
        } catch (Exception e) {
            throw new SQLDataException("Can't prepare value '" + value + "'", "preparing", e);
        }
    }

    public <T extends ISQLBatch> T cast(Class<T> iface) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return cast(iface, iface.getClassLoader());
    }

    public <T extends ISQLBatch> T cast(Class<T> iface, ClassLoader classLoader) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return create((Class<T>) classLoader.loadClass(getImplementation(iface)));
    }

    public <T extends ISQLBatch> T create(Class<T> implClass) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        T impl = (T) implClass.newInstance();
        impl.setMapper(this);
        impl.setDialect(provider.getDialect(impl.getConnectionName()));
        return impl;
    }

    public String getImplementation(Class<? extends ISQLBatch> iface) {
        return iface.getName() + "Impl";
    }

    public SqlStatement prepareStatement(String name, String sql, java.sql.Connection connection) throws SQLException {
        SqlStatement statement;
        if (getState() == SqlState.InTransaction) {
            statement = getStatementsCache().get(sql);
            if (statement == null) {
                statement = new SqlStatement(this, name, sql);
                getStatementsCache().put(sql, statement);
            }
        } else {
            statement = new SqlStatement(this, name, sql);
        }
        return statement;
    }

    public void releaseStatement(SqlStatement statement) {
        if (getState() != SqlState.InTransaction) {
            statement.close();
        }
    }

    private SqlState getState() {
        if (state.get() == null) {
            state.set(SqlState.OnDemand);
        }
        return state.get();
    }

    private void setState(SqlState state) {
        this.state.set(state);
    }

    private HashMap<String, java.sql.Connection> getConnectionsCache() {
        HashMap<String, java.sql.Connection> cache = connectionCache.get();
        if (cache == null) {
            cache = new HashMap<String, Connection>();
            connectionCache.set(cache);
        }
        return cache;
    }

    private HashMap<String, SqlStatement> getStatementsCache() {
        HashMap<String, SqlStatement> cache = statementCache.get();
        if (cache == null) {
            cache = new HashMap<String, SqlStatement>();
            statementCache.set(cache);
        }
        return cache;
    }

    private java.sql.Connection _takeConnection(String aliase) throws SQLException {
        return provider.takeConnection(aliase);
    }

    public void releaseConnectionNow(java.sql.Connection connection) {
        provider.releaseConnection(connection);
    }

    public java.sql.Connection takeConnection(String aliase) throws SQLException {
        java.sql.Connection connection;
        if (getState() == SqlState.InTransaction) {
            connection = getConnectionsCache().get(aliase);
            if (connection == null) {
                connection = _takeConnection(aliase);
                connection.setAutoCommit(false);
                getConnectionsCache().put(aliase, connection);
            }
        } else {
            connection = _takeConnection(aliase);
            connection.setAutoCommit(true);
        }
        return connection;
    }

    public void releaseConnection(java.sql.Connection connection) {
        if (getState() != SqlState.InTransaction) {
            releaseConnectionNow(connection);
        }
    }

    public void beginTransaction() {
        if (getState() != SqlState.InTransaction) {
            getStatementsCache().clear();
        }
        setState(SqlState.InTransaction);
    }

    private void closeTransaction(boolean commit) {
        for (SqlStatement statement : getStatementsCache().values()) {
            statement.close();
        }

        for (java.sql.Connection connection : getConnectionsCache().values()) {
            try {
                if (commit) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
            } catch (SQLException e) {
                e.printStackTrace(System.err);
            }
            releaseConnectionNow(connection);
        }
        setState(SqlState.OnDemand);
        getConnectionsCache().clear();
    }

    public void commitTransaction() {
        closeTransaction(true);
    }

    public void rollbackTransaction() {
        closeTransaction(false);
    }

    private boolean isPrimitive(Class clazz) {
        return clazz.isPrimitive()
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == BigDecimal.class
                || clazz == Double.class
                || clazz == String.class
                || Date.class.isAssignableFrom(clazz);
    }

    public <T> T createObject(Class<T> clazz, final ResultSet rs) throws SQLException {
        try {
            final HashMap<String, Class> colIndex = new HashMap();
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 0; i < meta.getColumnCount(); i++) {
                colIndex.put(meta.getColumnLabel(i + 1), resolve(meta.getColumnType(i + 1)));
            }

            return provider.createObject(clazz, new ISQLData() {
                public boolean isFetched() {
                    return true;
                }

                public boolean exists(String name) {
                    return colIndex.containsKey(name);
                }

                public Class getType(String name) throws SQLException {
                    return colIndex.get(name);
                }

                public Object getObject(String name) throws SQLException {
                    return rs.getObject(name);
                }
            });
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public <T> T convertTo(Object value, Class<T> clazz) throws SQLException {
        try {
            return provider.convertValue(value, clazz);
        } catch (Exception e) {
            throw new SQLDataException("Can't convert '" + value + "' into class '" + clazz.getName() + "'", "convertation", e);
        }
    }

    public <T> T returnValue(ResultSet rs, String colName, Class<T> convertTo) throws SQLException {
        if (isPrimitive(convertTo)) {
            return convertTo(rs.getObject(colName), convertTo);
        } else {
            return createObject(convertTo, rs);
        }
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    public <T> T[] createArray(ResultSet rs, String colName, Class<T> clazz) throws SQLException {
        try {
            ArrayList list = new ArrayList();
            while (rs.next()) {
                if (Object.class == clazz) {
                    list.add(rs.getObject(colName));
                } else if (isPrimitive(clazz)) {
                    list.add(provider.convertValue(rs.getObject(colName), clazz));
                } else {
                    list.add(createObject(clazz, rs));
                }
            }
            return (T[]) list.toArray((Object[]) Array.newInstance(clazz, list.size()));
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public ISQLDataTable createTable(ResultSet rs, Class<? extends ISQLDataTable> tableClass) throws SQLException {
        ISQLDataTable table = provider.createTable(tableClass);
        if (table == null) {
            table = new SqlDefaultDataTable(rs, this);
        } else {
            table.setMapper(this);
            table.reload(rs);
        }
        return table;
    }

    public ISQLDataRow createRow(ResultSet rs, Class<? extends ISQLDataRow> rowClass) throws SQLException {
        ISQLDataRow row = provider.createRow(rowClass);
        if (row == null) {
            row = new SqlDefaultDataRow(rs, this);
        } else {
            row.setMapper(this);
            row.reload(rs);
        }
        return row;
    }

    public ISQLDataParams createParams(SqlStatement statement, Class<? extends ISQLDataParams> paramClass, Class<? extends ISQLDataTable> tableClass) throws SQLException {
        ISQLDataParams params = provider.createParams(paramClass);
        if (params == null) {
            params = new SqlDefaultDataParams(this);
        } else {
            params.setMapper(this);
        }

        SQLException error = null;

        ResultSet rs;
        do {
            try {
                rs = statement.nextResult();
                if (rs == null) {
                    break;
                } else {
                    params.append(createTable(rs, tableClass));
                }
            } catch (SQLException e) {
                if (error == null) {
                    error = e;
                } else {
                    error.setNextException(e);
                }
            }

        } while (true);
        statement.extract(params);

        if (error != null) {
            throw error;
        }

        return params;
    }

    public boolean abortTransaction() throws SQLException {
        if (getState() == SqlState.InTransaction) {
            closeTransaction(false);
            return true;
        }
        return false;
    }

    public boolean inTransaction() {
        return getState() == SqlState.InTransaction;
    }

    public void transaction(final ILogic logic) throws SQLException {
        transaction(new IReturnableLogic<Object>() {
            public Object run() throws Exception {
                logic.run();
                return null;
            }
        });
    }

    @SuppressWarnings({"TooBroadCatch", "UseSpecificCatch"})
    public <T> T transaction(IReturnableLogic<T> logic) throws SQLException {
        if (getState() == SqlState.InTransaction) {
            try {
                return logic.run();
            } catch (Throwable e) {
                throw new SQLException(e);
            }
        } else {
            beginTransaction();
            try {
                T result = logic.run();
                commitTransaction();
                return result;
            } catch (Throwable e) {
                rollbackTransaction();
                throw new SQLException(e);
            }
        }
    }

}
