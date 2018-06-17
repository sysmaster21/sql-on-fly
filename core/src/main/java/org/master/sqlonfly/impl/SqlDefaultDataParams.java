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
public class SqlDefaultDataParams extends SqlAbstractDataParams {

    public SqlDefaultDataParams() {
    }

    public SqlDefaultDataParams(SQLEngine mapper) throws SQLException {
        setMapper(mapper);
    }

}
