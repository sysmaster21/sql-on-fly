/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.impl;

import org.master.sqlonfly.core.SQLEngine;
import org.master.sqlonfly.interfaces.ISQLDataParams;
import org.master.sqlonfly.interfaces.ISQLDataTable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author RogovA
 */
public abstract class SqlAbstractDataParams extends ConcurrentHashMap<String, Object> implements ISQLDataParams {

    private boolean fetched;
    private SQLEngine mapper;
    private final ConcurrentHashMap<String, Class> types = new ConcurrentHashMap<String, Class>();
    private final ArrayList<ISQLDataTable> tables = new ArrayList<ISQLDataTable>();

    public void putParameter(String name, int sqlType, Object value) {
        put(name, value);
        types.put(name, getMapper().resolve(sqlType));
    }

    public Iterable<ISQLDataTable> getTables() {
        return tables;
    }

    public void append(ISQLDataTable table) throws SQLException {
        tables.add(table);
    }

    public SQLEngine getMapper() {
        return mapper;
    }

    public void setMapper(SQLEngine mapper) {
        this.mapper = mapper;
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
