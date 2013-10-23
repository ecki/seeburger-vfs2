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
    /** Cached quoted identifier, possibly containing schema name. */
    private String quotedTable;


    /**
     * Create dialect for SQL server with default table name.
     * <P>
     * {@link #getQuotedTable()} will return <code>"[" + TABLE_NAME +"]"</code>.
     *
     * @param ds data source providing JDBC connections
     *
     * @see JdbcDialectBase#TABLE_NAME
     */
    public JdbcDialectMSSQL(DataSource ds)
    {
        this(TABLE_NAME, ds);
    }

    /**
     * Create dialect for SQL server with table name specified.
     * <P>
     * {@link #getQuotedTable()} will return <code>"[" + tableName +"]"</code>.
     *
     * @param tableName name of the table where the blobs are stored
     * @param ds data source providing JDBC connections
     */
    public JdbcDialectMSSQL(String tableName, DataSource ds)
    {
        super(tableName, ds);
        this.quotedTable = "[" + tableName + "]";
    }


    /**
     * Create dialect for SQL server with schema and table name specified.
     * <P>
     * {@link #getQuotedTable()} will return <code>"[" + schemaName + "].[" + tableName +"]"</code>.
     *
     * @param schemaName the schema name where the table lives
     * @param tableName name of the table where the blobs are stored
     * @param ds data source providing JDBC connections
     *
     * @see #getQuotedTable()
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
     *  @see #JdbcDialectMSSQL(String, DataSource)
     *  @see #JdbcDialectMSSQL(DataSource)
     *  @see JdbcDialectBase#TABLE_NAME
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
        String cached = sqlCache.get(sql);

        if (cached != null)
        {
            return cached;
        }

        String created = sql.replaceAll("\\{table\\}", getQuotedTable());
        created = created.replaceAll(" ?\\{FOR UPDATE\\}", "");

        sqlCache.put(sql, created);

        return created;
    }

    @Override
    public JdbcDialect cloneDialect(String tableName)
    {
        return new JdbcDialectMSSQL(tableName, dataSource);
    }
}

