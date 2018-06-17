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
public interface IConnectionProvider {

    String getDialect(String aliase);

    java.sql.Connection takeConnection(String aliase) throws SQLException;

    void releaseConnection(java.sql.Connection connection);

    <T> T convertValue(Object value, Class<T> convertTo) throws Exception;

    Object prepareValue(Object value) throws Exception;

    void afterBatchExecute(ISQLBatch batch, String methodName) throws InterruptedException;

    void beforeBatchExecute(ISQLBatch batch, String methodName);

    Class resolve(ISQLBatch.DataTypes type);

    <T> T createObject(Class<T> clazz, ISQLData data) throws Exception;

    ISQLDataTable createTable(Class<? extends ISQLDataTable> tableClass);

    ISQLDataRow createRow(Class<? extends ISQLDataRow> rowClass);

    ISQLDataParams createParams(Class<? extends ISQLDataParams> paramsClass);

    void debug(String msg);

    void debugQuery(String statementName, String query);
}
