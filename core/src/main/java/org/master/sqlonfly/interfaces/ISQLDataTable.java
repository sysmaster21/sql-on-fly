/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.interfaces;

import org.master.sqlonfly.core.SQLEngine;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 *
 * @author RogovA
 * @param <R>
 */
public interface ISQLDataTable<R extends ISQLDataRow> extends ISQLGetter {

    SQLEngine getMapper();

    void setMapper(SQLEngine mapper);

    void reload(java.sql.ResultSet rs) throws SQLException;

    int count();

    void reset();

    boolean next() throws SQLException;

    R row() throws SQLException;

    String[] columns();

    int getColumnIndex(String name);

    Object getObject(int index) throws SQLException;

    String getString(int index) throws SQLException;

    Integer getInteger(int index) throws SQLException;

    Long getLong(int index) throws SQLException;

    java.util.Date getDateTime(int index) throws SQLException;

    BigDecimal getDecimal(int index) throws SQLException;

    Double getDouble(int index) throws SQLException;

    byte[] getBinary(int index) throws SQLException;

}
