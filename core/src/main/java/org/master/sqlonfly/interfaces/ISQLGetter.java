/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.interfaces;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 *
 * @author RogovA
 */
public interface ISQLGetter extends ISQLData {

    String getString(String name) throws SQLException;

    Integer getInteger(String name) throws SQLException;

    Long getLong(String name) throws SQLException;

    java.util.Date getDateTime(String name) throws SQLException;

    BigDecimal getDecimal(String name) throws SQLException;

    Double getDouble(String name) throws SQLException;

    byte[] getBinary(String name) throws SQLException;

}
