/*
 * JdbcDialectOracle.java
 *
 * created at 2013-09-13 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import javax.sql.DataSource;



/**
 * Oracle Database specific dialect for JdbcTable VFS Provider.
 * <P>
 * Tested with JTHIN 11.2 and 11.1 drivers.
 */
public class JdbcDialectOracle extends JdbcDialectBase implements JdbcDialect
{
    private String quotedTable;

    public JdbcDialectOracle(DataSource ds)
    {
        this("tBlobs", ds);
    }

    public JdbcDialectOracle(String name, DataSource ds)
    {
        super(name, ds);
        this.quotedTable = "\"" + name + "\"";
    }

    @Override
    public String getQuotedTable()
    {
        return quotedTable;
    }

    @Override
    public boolean supportsAppendBlob()
    {
        return true;
    }

    // public String expandSQL(String sql) -- default works for Oracle (using FOR UPDATE)
}



