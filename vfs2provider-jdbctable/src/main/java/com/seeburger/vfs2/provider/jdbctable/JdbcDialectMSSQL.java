/*
 * JdbcDialectMSSQL.java
 *
 * created at 2013-09-13 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import javax.sql.DataSource;

import com.seeburger.vfs2.provider.jdbctable.JdbcDialect;
import com.seeburger.vfs2.provider.jdbctable.JdbcDialectBase;


/**
 * Microsoft SQL Server specific dialect for JdbcTable VFS Provider.
 * <P>
 * Tested with JTDS driver.
 */
public class JdbcDialectMSSQL extends JdbcDialectBase implements JdbcDialect
{
    private String quotedTable;

    public JdbcDialectMSSQL(DataSource ds)
    {
        this("tBlobs", ds);
    }

    public JdbcDialectMSSQL(String tableName, DataSource ds)
    {
        super(tableName, ds);
        this.quotedTable = "[" + tableName + "]";
    }

    public JdbcDialectMSSQL(String schemaName, String tableName, DataSource ds)
    {
        super(tableName, ds);
        this.quotedTable = "[" + schemaName + "].[" + tableName + "]";
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

    public String expandSQL(String sql)
    {
        String cached = sqlCache.get(sql);

        if (cached != null)
            return cached;

        synchronized(this)
        {
            String created = sql.replaceAll("\\{table\\}", getQuotedTable());
            created = created.replaceAll(" ?\\{FOR UPDATE\\}", "");
            // System.out.printf("  %s%n    %s%n", sql, created);
            sqlCache.put(sql, created);
            return created;
        }
    }
}



