/*
 * JdbcDialectBase.java
 *
 * created at 2013-09-13 by Bernd Eckenfel <b.eckenfes@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;


/**
 * Base implementation of {@link JdbcDialect} interface.
 * <P>
 * Can be used for H2 and Derby databases. There are some more specific subclasses. Own implementations should
 * extend {@link JdbcDialectBase}.
 *
 * @see JdbcDialectBase
 * @see JdbcDialectOracle
 * @see JdbcDialectMSSQL
 */
public class JdbcDialectBase
    implements JdbcDialect
{
    private static final String ORACLE_DIALECT = "oracle";
    private static final String MSSQL_DIALECT$ = "micro$oft";
    private static final String MSSQL_DIALECT = "microsoft";
    static final String TABLE_NAME = "tBlobs";
    static final int MS = 1000000;

    final protected String tableName;
    final protected DataSource dataSource;

    final protected ConcurrentMap<String, String> sqlCache = new ConcurrentHashMap<String, String>(23);

    /**
     * Determines what the type of the provided datasource is and returns the appropriate JdbcDialect. Currently supporting only MSSQL and Oracle.
     * @param ds
     * @return
     * @throws SQLException
     */
    public static JdbcDialect getDialect(DataSource ds)
        throws SQLException
    {
        JdbcDialect result = null;
        Connection con = null;
        try
        {
            con = ds.getConnection();
            DatabaseMetaData md = con.getMetaData();
            String provider = md.getDatabaseProductName().toLowerCase();
            if (provider.contains(MSSQL_DIALECT) || provider.contains(MSSQL_DIALECT$))
            {
                result = new JdbcDialectMSSQL(ds);
            }
            else if (provider.contains(ORACLE_DIALECT))
            {
                result = new JdbcDialectOracle(ds);
            }
            else
            {
                result = new JdbcDialectBase(ds);
            }

            return result;
        }
        finally
        {
            if (con != null)
            {
                con.close();
            }
        }
    }


    /** Create a database description for the given datasource with a specifc table name. */
    public JdbcDialectBase(String tableName, DataSource dataSource)
    {
        this.tableName = tableName;
        this.dataSource = dataSource;
    }


    public JdbcDialectBase(DataSource dataSource)
    {
        this(TABLE_NAME, dataSource);
    }


    /** borrow a new connection from pool (or create one). */
    @Override
    public Connection getConnection()
        throws SQLException
    {
        long start = System.nanoTime();
        Connection c = dataSource.getConnection();
        long duration = System.nanoTime() - start;
        if (duration > 100 * MS)
            System.out.printf("slow getConnection(): %.6fs%n", (duration / 1000000000.0)); // TODO: logging?
        return c;
    }


    /** resturn connection to the pool (most often this will only call close(). */
    @Override
    public void returnConnection(Connection c)
    {
        try
        {
            c.close();
        }
        catch (Exception ignored)
        {}
    }


    /** {@inheritDoc} */
    @Override
    public String getQuotedTable()
    {
        return tableName;
    }


    /** {@inheritDoc} */
    @Override
    public String expandSQL(String sql)
    {
        String cached = sqlCache.get(sql);

        if (cached != null)
            return cached;

        synchronized (this)
        {
            String created = sql.replaceAll("\\{table\\}", getQuotedTable());
            created = created.replaceAll(" ?\\{FOR UPDATE\\}", " FOR UPDATE");
            sqlCache.put(sql, created);
            return created;
        }
    }


    /** {@inheritDoc} */
    @Override
    public PreparedStatement prepareQuery(Connection con, String sql)
        throws SQLException
    {
        String expanded = expandSQL(sql);
        return con.prepareStatement(expanded); // TYPE_FORWARD_ONLY, CONCUR_READ_ONLY
    }


    /** {@inheritDoc} */
    @Override
    public PreparedStatement prepareUpdateable(Connection con, String sql)
        throws SQLException
    {
        String expanded = expandSQL(sql);
        return con.prepareStatement(expanded, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
    }


    /** {@inheritDoc} */
    @Override
    public boolean supportsAppendBlob()
    {
        return false;
    }
}
