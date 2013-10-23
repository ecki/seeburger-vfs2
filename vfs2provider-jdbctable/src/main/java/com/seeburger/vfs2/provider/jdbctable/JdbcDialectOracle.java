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


    /**
     * Create dialect for Oracle server with default table name {@value #TABLE_NAME}.
     * <P>
     * {@link #getQuotedTable()} will return <code>"\"" + TABLE_NAME + "\""</code>.
     *
     * @param ds the data source to get JDBC connections
     *
     * @see #getQuotedTable()
     * @see JdbcDialectBase#TABLE_NAME
     */
    public JdbcDialectOracle(DataSource ds)
    {
        this(TABLE_NAME, ds);
    }

    /**
     * Create dialect for Oracle server with table name specified.
     * <P>
     * {@link #getQuotedTable()} will return <code>"\"" + tableName + "\""</code>.
     *
     * @param ds the data source to get JDBC connections
     * @param tableName the name of the table
     *
     * @see #getQuotedTable()
     * @see JdbcDialectBase#TABLE_NAME
     */
    public JdbcDialectOracle(String tableName, DataSource ds)
    {
        super(tableName, ds);
        this.quotedTable = "\"" + tableName + "\"";
    }

    /**
     * {@inheritDoc}
     * <P>
     * On Oracle identifiers get quoted like " " (which makes them case sensitive).
     *
     *  @see #JdbcDialectOracle(String, DataSource)
     *  @see #JdbcDialectOracle(DataSource)
     */
    public String getQuotedTable()
    {
        return quotedTable;
    }

    /**
     * {@inheritDoc}
     * <P>
     * Oracle/OThin supports appending blobs.
     */
    @Override
    public boolean supportsAppendBlob()
    {
        return true;
    }

    @Override
    public JdbcDialect cloneDialect(String tableName)
    {
        return new JdbcDialectOracle(tableName, dataSource);
    }

    // public String expandSQL(String sql) -- default works for Oracle (using FOR UPDATE)
}

