/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.interfaces;

import org.master.sqlonfly.impl.SqlStatement;
import java.sql.SQLException;

/**
 *
 * @author RogovA
 * @param <R>
 */
public interface ISQLDirectTable<R extends ISQLDataRow> extends ISQLDataTable<R>, AutoCloseable {

    void init(SqlStatement statement, java.sql.Connection connection);

    boolean nextResultSet() throws SQLException;
}
