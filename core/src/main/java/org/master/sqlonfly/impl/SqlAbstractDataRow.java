/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.impl;

import org.master.sqlonfly.core.SQLEngine;
import org.master.sqlonfly.interfaces.ISQLDataRow;
import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author RogovA
 */
public abstract class SqlAbstractDataRow extends ConcurrentHashMap<String, Object> implements ISQLDataRow {

    private boolean fetched;
    private SQLEngine mapper;
    private final ConcurrentHashMap<String, Class> types = new ConcurrentHashMap<String, Class>();

    public SQLEngine getMapper() {
        return mapper;
    }

    public void setMapper(SQLEngine mapper) {
        this.mapper = mapper;
    }

    public void reload(java.sql.ResultSet rs) throws SQLException {
        if (rs != null && rs.next()) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                Object obj = rs.getObject(i);
                if (obj != null) {
                    String name = meta.getColumnLabel(i);
                    put(name, obj);
                    types.put(name, getMapper().resolve(meta.getColumnType(i)));
                }
            }
            fetched = true;
        } else {
            fetched = false;
        }
    }

    public void reload(SQLColumn[] columns, Object[] data) {
        if (columns != null && data != null && columns.length == data.length) {
            for (int i = 0; i < data.length; i++) {
                if (data[i] != null && columns[i].getName() != null) {
                    put(columns[i].getName(), data[i]);
                    types.put(columns[i].getName(), columns[i].getType());
                }
            }
            fetched = true;
        } else {
            fetched = false;
        }
    }

    public boolean isFetched() {
        return fetched;
    }

    public boolean exists(String name) {
        return containsKey(name);
    }

    public Class getType(String name) throws SQLException {
        return types.get(name);
    }

    public Object getObject(String name) throws SQLException {
        return get(name);
    }

    public String getString(String name) throws SQLException {
        return mapper.convertTo(getObject(name), String.class);
    }

    public Integer getInteger(String name) throws SQLException {
        return mapper.convertTo(getObject(name), Integer.class);
    }

    public Long getLong(String name) throws SQLException {
        return mapper.convertTo(getObject(name), Long.class);
    }

    public Date getDateTime(String name) throws SQLException {
        return mapper.convertTo(getObject(name), Date.class);
    }

    public BigDecimal getDecimal(String name) throws SQLException {
        return mapper.convertTo(getObject(name), BigDecimal.class);
    }

    public Double getDouble(String name) throws SQLException {
        return mapper.convertTo(getObject(name), Double.class);
    }

    public byte[] getBinary(String name) throws SQLException {
        return mapper.convertTo(getObject(name), byte[].class);
    }

}
