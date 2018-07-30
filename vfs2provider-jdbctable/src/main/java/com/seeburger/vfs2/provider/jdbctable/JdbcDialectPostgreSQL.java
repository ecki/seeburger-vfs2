/*
 * JdbcDialectPostgreSQL.java
 *
 * created at 2018-07-27 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import javax.sql.DataSource;


/**
 * PostgreSQL Database specific dialect for JdbcTable VFS Provider.
 */
public class JdbcDialectPostgreSQL extends JdbcDialectBase implements JdbcDialect
{
    private String quotedTable;


    /**
     * Create dialect for PostgreSQL server with default table name {@value #TABLE_NAME}.
     * <P>
     * {@link #getQuotedTable()} will return <code>"\"" + TABLE_NAME + "\""</code>.
     *
     * @param ds the data source to get JDBC connections
     *
     * @see #getQuotedTable()
     * @see JdbcDialectBase#TABLE_NAME
     */
    public JdbcDialectPostgreSQL(DataSource ds)
    {
        this(TABLE_NAME, ds);
    }

    /**
     * Create dialect for PostgreSQL server with table name specified.
     * <P>
     * {@link #getQuotedTable()} will return <code>"\"" + tableName + "\""</code>.
     *
     * @param ds the data source to get JDBC connections
     * @param tableName the name of the table
     *
     * @see #getQuotedTable()
     * @see JdbcDialectBase#TABLE_NAME
     */
    public JdbcDialectPostgreSQL(String tableName, DataSource ds)
    {
        super(tableName, ds);
        this.quotedTable = "\"" + tableName.toLowerCase(Locale.ROOT) + "\"";
    }

    /**
     * {@inheritDoc}
     * <P>
     * On PostgreSQL identifiers get quoted like " " (which makes them case sensitive).
     *
     *  @see #JdbcDialectPostgreSQL(String, DataSource)
     *  @see #JdbcDialectPostgreSQL(DataSource)
     */
    public String getQuotedTable()
    {
        return quotedTable;
    }

    @Override
    public boolean supportsBlob()
    {
        return false;
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        Connection c = super.getConnection();
        c.setAutoCommit(false); // TODO
        return c;
    }

    @Override
    public JdbcDialect cloneDialect(String tableName)
    {
        return new JdbcDialectPostgreSQL(tableName, dataSource);
    }

    // public String expandSQL(String sql) -- default works for PostgreSQL (using FOR UPDATE)
}

