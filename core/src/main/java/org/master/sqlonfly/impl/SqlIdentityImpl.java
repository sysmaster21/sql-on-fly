/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.impl;

import org.master.sqlonfly.interfaces.ISQLIdentity;

/**
 *
 * @author RogovA
 */
public class SqlIdentityImpl implements ISQLIdentity {

    private final long value;

    public SqlIdentityImpl(long value) {
        this.value = value;
    }

    public long get() {
        return value;
    }

}
