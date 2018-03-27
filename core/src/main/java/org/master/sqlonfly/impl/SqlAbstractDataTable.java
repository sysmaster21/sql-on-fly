/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.impl;

import org.master.sqlonfly.core.SQLEngine;
import org.master.sqlonfly.interfaces.ISQLDataRow;
import org.master.sqlonfly.interfaces.ISQLDataTable;
import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author RogovA
 * @param <R>
 */
public abstract class SqlAbstractDataTable<R extends ISQLDataRow> implements ISQLDataTable<R> {

    private final HashMap<String, Integer> colIndex = new HashMap();
    private SQLColumn[] columns;
    private SQLEngine mapper;

    public boolean exists(String name) {
        return colIndex.containsKey(name);
    }

    protected SQLColumn[] getColumns() {
        return columns;
    }

    public SQLEngine getMapper() {
        return mapper;
    }

    public void setMapper(SQLEngine mapper) {
        this.mapper = mapper;
    }

    protected abstract void fill(java.sql.ResultSet rs) throws SQLException;

    public void reload(java.sql.ResultSet rs) throws SQLException {
        if (rs != null) {
            ResultSetMetaData meta = rs.getMetaData();
            columns = new SQLColumn[meta.getColumnCount()];
            for (int i = 0; i < columns.length; i++) {
                SQLColumn col = new SQLColumn(meta.getColumnLabel(i + 1), getMapper().resolve(meta.getColumnType(i + 1)));
                columns[i] = col;
                colIndex.put(col.getName(), i);
            }
        }
        fill(rs);
        reset();
    }

    public String[] columns() {
        String[] names = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            names[i] = columns[i].getName();
        }
        return names;
    }

    public int getColumnIndex(String name) {
        Integer index = colIndex.get(name);
        if (index == null) {
            return -1;
        } else {
            return index;
        }
    }

    public Class getType(String name) throws SQLException {
        Integer index = colIndex.get(name);
        if (index == null || index < 0 || index >= columns.length) {
            throw new SQLDataException("Invalid columns name: " + name);
        }
        return columns[index].getType();
    }

    public String getString(int index) throws SQLException {
        return mapper.convertTo(getObject(index), String.class);
    }

    public String getString(String name) throws SQLException {
        return mapper.convertTo(getObject(name), String.class);
    }

    public Integer getInteger(int index) throws SQLException {
        return mapper.convertTo(getObject(index), Integer.class);
    }

    public Integer getInteger(String name) throws SQLException {
        return mapper.convertTo(getObject(name), Integer.class);
    }

    public Long getLong(int index) throws SQLException {
        return mapper.convertTo(getObject(index), Long.class);
    }

    public Long getLong(String name) throws SQLException {
        return mapper.convertTo(getObject(name), Long.class);
    }

    public Date getDateTime(int index) throws SQLException {
        return mapper.convertTo(getObject(index), Date.class);
    }

    public Date getDateTime(String name) throws SQLException {
        return mapper.convertTo(getObject(name), Date.class);
    }

    public BigDecimal getDecimal(int index) throws SQLException {
        return mapper.convertTo(getObject(index), BigDecimal.class);
    }

    public BigDecimal getDecimal(String name) throws SQLException {
        return mapper.convertTo(getObject(name), BigDecimal.class);
    }

    public Double getDouble(int index) throws SQLException {
        return mapper.convertTo(getObject(index), Double.class);
    }

    public Double getDouble(String name) throws SQLException {
        return mapper.convertTo(getObject(name), Double.class);
    }

    public byte[] getBinary(int index) throws SQLException {
        return mapper.convertTo(getObject(index), byte[].class);
    }

    public byte[] getBinary(String name) throws SQLException {
        return mapper.convertTo(getObject(name), byte[].class);
    }

}
