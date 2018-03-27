/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.impl;

import org.master.sqlonfly.core.SQLEngine;
import java.sql.SQLException;

/**
 *
 * @author RogovA
 */
public class SqlDefaultDataRow extends SqlAbstractDataRow {

    public SqlDefaultDataRow() {
    }
    
    public SqlDefaultDataRow(java.sql.ResultSet rs, SQLEngine mapper) throws SQLException {
        setMapper(mapper);
        reload(rs);
    }

    public SqlDefaultDataRow(SQLColumn[] columns, Object[] data, SQLEngine mapper) {
        setMapper(mapper);
        reload(columns, data);
    }

}
