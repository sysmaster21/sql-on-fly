/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.interfaces;

import org.master.sqlonfly.core.SQLEngine;
import java.sql.SQLException;
import java.util.Map;

/**
 *
 * @author RogovA
 */
public interface ISQLDataParams extends Map<String, Object>, ISQLGetter {

    SQLEngine getMapper();

    void setMapper(SQLEngine mapper);

    Iterable<ISQLDataTable> getTables();

    void append(ISQLDataTable table) throws SQLException;
    
    void putParameter(String name, int sqlType, Object value);

}
