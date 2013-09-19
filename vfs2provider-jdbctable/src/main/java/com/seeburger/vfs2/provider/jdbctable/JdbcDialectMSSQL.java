/*
 * JdbcDialectMSSQL.java
 *
 * created at 2013-09-13 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved. TODO
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
    /** Cached quoted identifier, possibly containing schema name. */
    private String quotedTable;


    /**
     * Create dialect for SQL server with default table name {@value #TABLE_NAME}.
     *
     * @param ds
     */
    public JdbcDialectMSSQL(DataSource ds)
    {
        this(TABLE_NAME, ds);
    }

    /**
     * Create dialect for SQL server with table name specified.
     *
     * @param tableName
     * @param ds
     */
    public JdbcDialectMSSQL(String tableName, DataSource ds)
    {
        super(tableName, ds);
        this.quotedTable = "[" + tableName + "]";
    }

    /**
     * Create dialect for SQL server with schema and table name specified.
     *
     * @param schemaName
     * @param tableName
     * @param ds
     */
    public JdbcDialectMSSQL(String schemaName, String tableName, DataSource ds)
    {
        super(tableName, ds);
        this.quotedTable = "[" + schemaName + "].[" + tableName + "]";
    }

    /**
     * {@inheritDoc}
     * <P>
     * On MSSQL identifiers get quoted like [] and the name may optionally
     * include a (quoted) schema name.
     *
     *  @see #JdbcDialectMSSQL(String, String, DataSource)
     */
    @Override
    public String getQuotedTable()
    {
        return quotedTable;
    }

    /**
     * {@inheritDoc}
     * <P>
     * MSSQL/jTDS supports appending blobs.
     */
    @Override
    public boolean supportsAppendBlob()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * <P>
     * The MSSQL specific version will remove " {{FOR UPDATE}}".
     * */
    @Override
    public String expandSQL(String sql)
    {
        String cached = sqlCache.get(sql); // Concurrent map -> happens after satisfied

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

