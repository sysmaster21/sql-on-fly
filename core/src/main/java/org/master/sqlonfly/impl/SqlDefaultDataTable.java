/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.impl;

import org.master.sqlonfly.core.SQLEngine;
import org.master.sqlonfly.interfaces.ISQLDataRow;
import java.sql.SQLException;

/**
 *
 * @author RogovA
 */
public class SqlDefaultDataTable extends SqlCachedTable<ISQLDataRow> {

    public SqlDefaultDataTable() {
    }

    public SqlDefaultDataTable(java.sql.ResultSet rs, SQLEngine mapper) throws SQLException {
        setMapper(mapper);
        reload(rs);
    }

    public ISQLDataRow row() {
        return new SqlDefaultDataRow(getColumns(), getRows()[getCurrent()], getMapper());
    }
}
