/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.impl;

import org.master.sqlonfly.interfaces.ISQLBatch;
import org.master.sqlonfly.core.SQLEngine;
import java.sql.SQLWarning;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author RogovA
 * @param <T>
 */
public abstract class AbstractSQLBatch<T extends ISQLBatch> implements ISQLBatch<T> {

    private final ThreadLocal<SQLWarning> warning = new ThreadLocal<SQLWarning>();
    private final ThreadLocal<Integer> rowcount = new ThreadLocal<Integer>();
    private SQLEngine mapper;
    private String dialect;
    private final String name;

    public AbstractSQLBatch(String name) {
        this.name = name;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public void setMapper(SQLEngine mapper) {
        this.mapper = mapper;
    }

    public SQLEngine getMapper() {
        return mapper;
    }

    public Integer getRowcount() {
        return rowcount.get();
    }

    public T rowcount(int rowcount) {
        if (rowcount <= 0) {
            this.rowcount.set(null);
        } else {
            this.rowcount.set(rowcount);
        }

        return (T) this;
    }

    public String like(String value) {
        if (value == null || value.isEmpty()) {
            return "%";
        } else {
            return value.replaceAll("\\*", "%").replaceAll("\\?", "_");
        }
    }

    public String any(String value) {
        if (value == null || value.isEmpty()) {
            return "%";
        } else {
            return "%" + value.replaceAll("\\*", "%").replaceAll("\\?", "_") + "%";
        }
    }

    public Date from(Date date) {
        date = date == null ? new Date() : date;
        Calendar cl = Calendar.getInstance();
        cl.setTime(date);
        cl.set(Calendar.HOUR, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        return cl.getTime();
    }

    public Date to(Date date) {
        date = date == null ? new Date() : date;
        Calendar cl = Calendar.getInstance();
        cl.setTime(date);
        cl.add(Calendar.DAY_OF_MONTH, 1);
        cl.set(Calendar.HOUR, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        return cl.getTime();
    }

    public SQLWarning getWarnings() {
        return warning.get();
    }

    public void setWarnings(SQLWarning warning) {
        this.warning.set(warning);
    }

    public String getName() {
        return name;
    }

}
