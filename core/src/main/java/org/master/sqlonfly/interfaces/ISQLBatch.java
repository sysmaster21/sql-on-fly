/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.interfaces;

import org.master.sqlonfly.core.SQLEngine;
import java.math.BigDecimal;
import java.sql.SQLWarning;
import java.util.Date;

/**
 *
 * @author RogovA
 * @param <T>
 */
public interface ISQLBatch<T extends ISQLBatch> {

    public enum DataTypes {

        INT,
        INTEGER,
        BIGINT,
        DATE,
        TIME,
        TIMESTAMP,
        CHAR,
        DECIMAL,
        DOUBLE,
        FLOAT,
        NCHAR,
        NUMERIC,
        NVARCHAR,
        VARCHAR,
        BLOB,
        CLOB,
        TEXT,
        DYNAMIC,
        MAP;

        public int getSqlType() {
            switch (this) {
                case BIGINT:
                    return java.sql.Types.BIGINT;
                case CHAR:
                    return java.sql.Types.CHAR;
                case DATE:
                    return java.sql.Types.DATE;
                case DECIMAL:
                    return java.sql.Types.DECIMAL;
                case DOUBLE:
                    return java.sql.Types.DOUBLE;
                case FLOAT:
                    return java.sql.Types.FLOAT;
                case INT:
                    return java.sql.Types.INTEGER;
                case INTEGER:
                    return java.sql.Types.INTEGER;
                case NCHAR:
                    return java.sql.Types.NCHAR;
                case NUMERIC:
                    return java.sql.Types.NUMERIC;
                case NVARCHAR:
                    return java.sql.Types.NVARCHAR;
                case TIME:
                    return java.sql.Types.TIME;
                case TIMESTAMP:
                    return java.sql.Types.TIMESTAMP;
                case TEXT:
                    return java.sql.Types.CLOB;
                case BLOB:
                    return java.sql.Types.BLOB;
                default:
                    return java.sql.Types.VARCHAR;
            }
        }

        public Class getJavaClass() {
            switch (this) {
                case BIGINT:
                    return Long.class;
                case CHAR:
                    return String.class;
                case DATE:
                    return java.util.Date.class;
                case DECIMAL:
                    return BigDecimal.class;
                case DOUBLE:
                    return Double.class;
                case FLOAT:
                    return Double.class;
                case INT:
                    return Integer.class;
                case INTEGER:
                    return Integer.class;
                case NCHAR:
                    return String.class;
                case NUMERIC:
                    return BigDecimal.class;
                case NVARCHAR:
                    return String.class;
                case TIME:
                    return java.util.Date.class;
                case TIMESTAMP:
                    return java.util.Date.class;
                case TEXT:
                    return String.class;
                case BLOB:
                    return byte[].class;
                default:
                    return String.class;
            }
        }

        public static DataTypes valueOf(int sqlType) {
            switch (sqlType) {
                case java.sql.Types.BIGINT:
                    return BIGINT;
                case java.sql.Types.CHAR:
                    return CHAR;
                case java.sql.Types.DATE:
                    return DATE;
                case java.sql.Types.DECIMAL:
                    return DECIMAL;
                case java.sql.Types.DOUBLE:
                    return DOUBLE;
                case java.sql.Types.FLOAT:
                    return FLOAT;
                case java.sql.Types.INTEGER:
                    return INTEGER;
                case java.sql.Types.NCHAR:
                    return NCHAR;
                case java.sql.Types.NUMERIC:
                    return NUMERIC;
                case java.sql.Types.NVARCHAR:
                    return NVARCHAR;
                case java.sql.Types.TIME:
                    return TIME;
                case java.sql.Types.TIMESTAMP:
                    return TIMESTAMP;
                case java.sql.Types.CLOB:
                    return TEXT;
                case java.sql.Types.BLOB:
                    return BLOB;
                default:
                    return VARCHAR;
            }
        }
    }

    String getDialect();

    void setDialect(String dialect);

    SQLEngine getMapper();

    void setMapper(SQLEngine mapper);

    String getConnectionName();

    T rowcount(int rowcount);

    String like(String value);

    Date from(Date date);

    Date to(Date date);

    SQLWarning getWarnings();

    String getName();

}
