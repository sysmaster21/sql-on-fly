/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.impl;

import org.master.sqlonfly.interfaces.ISQLDataRow;
import org.master.sqlonfly.interfaces.ISQLDirectTable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;

/**
 *
 * @author RogovA
 * @param <R>
 */
public abstract class SqlDirectTable<R extends ISQLDataRow> extends SqlAbstractDataTable<R> implements ISQLDirectTable<R> {

    private ResultSet rs;
    private SqlStatement statement;
    private java.sql.Connection connection;

    public void init(SqlStatement statement, Connection connection) {
        this.statement = statement;
        this.connection = connection;
    }

    @Override
    protected void fill(ResultSet rs) throws SQLException {
        this.rs = rs;
    }

    protected ResultSet data() {
        return rs;
    }

    public int count() {
        return -1;
    }

    public void reset() {
    }

    public boolean next() throws SQLException {
        return rs.next();
    }

    public Object getObject(int index) throws SQLException {
        if (index < 0 || index >= getColumns().length) {
            throw new SQLDataException("Invalid columns index: " + index);
        }
        return rs.getObject(index + 1);
    }

    public boolean isFetched() {
        return false;
    }

    public Object getObject(String name) throws SQLException {
        Integer index = getColumnIndex(name);
        if (index < 0 || index >= getColumns().length) {
            throw new SQLDataException("Invalid columns name: " + name);
        }
        return rs.getObject(index + 1);
    }

    public boolean nextResultSet() throws SQLException {
        rs = statement.nextResult();
        return rs != null;
    }

    public void close() throws Exception {
        try {
            rs.close();
        } catch (SQLException ignore) {
        }
        statement.close();
        getMapper().releaseConnectionNow(connection);
    }

}
