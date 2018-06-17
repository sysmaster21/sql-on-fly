/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.impl;

import org.master.sqlonfly.interfaces.ISQLDataRow;
import org.master.sqlonfly.interfaces.ISQLDirectTable;
import java.sql.SQLException;

/**
 *
 * @author RogovA
 */
public class SqlDefaultDirectTable extends SqlDirectTable<ISQLDataRow> implements ISQLDirectTable<ISQLDataRow> {

    public ISQLDataRow row() throws SQLException {
        return new SqlDefaultDataRow(data(), getMapper());
    }

}
