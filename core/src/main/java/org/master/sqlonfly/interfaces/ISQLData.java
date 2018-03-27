/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.interfaces;

import java.sql.SQLException;

/**
 *
 * @author RogovA
 */
public interface ISQLData {

    boolean isFetched();

    boolean exists(String name);

    Class getType(String name) throws SQLException;

    Object getObject(String name) throws SQLException;

}
