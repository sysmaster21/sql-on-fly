/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.impl;

import org.master.sqlonfly.interfaces.ISQLDataRow;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author RogovA
 * @param <R>
 */
public abstract class SqlCachedTable<R extends ISQLDataRow> extends SqlAbstractDataTable<R> {

    private Object[][] rows;
    private boolean fetched;
    private int current;

    protected Object[][] getRows() {
        return rows;
    }

    protected int getCurrent() {
        return current;
    }

    public void fill(java.sql.ResultSet rs) throws SQLException {
        fetched = false;
        ArrayList<Object[]> data = new ArrayList<Object[]>();
        while (rs != null && rs.next()) {
            Object[] row = new Object[getColumns().length];
            for (int i = 0; i < getColumns().length; i++) {
                row[i] = rs.getObject(i + 1);
            }
            data.add(row);
            fetched = true;
        }
        rows = data.toArray(new Object[data.size()][]);
        reset();
    }

    public int count() {
        return rows.length;
    }

    public boolean isFetched() {
        return fetched;
    }

    public final void reset() {
        current = -1;
    }

    public boolean next() {
        current++;
        return current < rows.length;
    }

    private int index() throws SQLException {
        if (!fetched) {
            throw new SQLDataException("No data fetched");
        } else if (current < 0 || current >= rows.length) {
            throw new SQLDataException("No more data in resultset");
        }
        return current;
    }

    public Object getObject(int index) throws SQLException {
        if (index < 0 || index >= getColumns().length) {
            throw new SQLDataException("Invalid columns index: " + index);
        }
        return rows[index()][index];
    }

    public Object getObject(String name) throws SQLException {
        Integer index = getColumnIndex(name);
        if (index < 0 || index >= getColumns().length) {
            throw new SQLDataException("Invalid columns name: " + name);
        }
        return rows[index()][index];
    }

}
