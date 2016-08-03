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
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;


/**
 * Base implementation of {@link JdbcDialect} interface.
 * <P>
 * Can be used for H2 and Derby databases. There are some more specific subclasses. Own implementations should
 * extend {@link JdbcDialectBase}.
 *
 * @see JdbcDialect
 * @see JdbcDialectOracle
 * @see JdbcDialectMSSQL
 */
public class JdbcDialectBase implements JdbcDialect
{
    private static final String ORACLE_DIALECT = "oracle";
    private static final String MSSQL_DIALECT$ = "micro$oft";
    private static final String MSSQL_DIALECT = "microsoft";
    private static final int MS = 1000000;

    /** Default table name. */
    protected static final String TABLE_NAME = "tBlobs";

    /** Cache for prepared statements. Not shared with other instances. */
    protected final ConcurrentMap<String, String> sqlCache = new ConcurrentHashMap<String, String>(23);

    protected final DataSource dataSource;
    protected final String tableName;


    /**
     * Determines what the type of the provided datasource is and
     * returns the appropriate JdbcDialect.
     * <P>
     * Returns {@link JdbcDialectMSSQL} for Microsoft database, {@link JdbcDialectOracle}
     * for Oracle drivers and {@link JdbcDialectBase} for all others.
     *
     * @param ds returning one of the know database driver connections
     * @return new instance of dialect with default table name configured
     * @throws SQLException
     *
     * @see JdbcDialectBase#JdbcDialectBase(DataSource)
     * @see DatabaseMetaData#getDatabaseProductName()
     */
    public static JdbcDialect getDialect(DataSource ds)
        throws SQLException
    {
        Connection con = null;
        try
        {
            con = ds.getConnection();
            DatabaseMetaData md = con.getMetaData();
            String provider = md.getDatabaseProductName().toLowerCase(Locale.ENGLISH);
            if (provider.contains(MSSQL_DIALECT) || provider.contains(MSSQL_DIALECT$))
            {
                return new JdbcDialectMSSQL(ds);
            }
            else if (provider.contains(ORACLE_DIALECT))
            {
                return new JdbcDialectOracle(ds);
            }
            else
            {
                return new JdbcDialectBase(ds);
            }
        }
        finally
        {
            safeClose(con);
        }
    }


    /** Create base dialect with default table name {@value JdbcDialectBase#TABLE_NAME}. */
    public JdbcDialectBase(DataSource dataSource)
    {
        this(TABLE_NAME, dataSource);
    }

    /** Create base dialect with specified table name. */
    public JdbcDialectBase(String tableNameArg, DataSource dataSourceArg)
    {
        this.tableName = tableNameArg;
        this.dataSource = dataSourceArg;
    }


    /** Borrow a new connection from pool (or create one). */
    @Override
    public Connection getConnection()
        throws SQLException
    {
        long start = System.nanoTime();
        try
        {
            return dataSource.getConnection();
        }
        finally
        {
            long duration = System.nanoTime() - start;
            if (duration > 100 * MS)
            {
                System.out.printf("slow getConnection(): %.6fs%n", Double.valueOf((duration / 1000000000.0))); // TODO: logging
            }
        }
    }


    /**
     * Return connection to the pool.
     * <P>
     * For the base implementation this will {@link #safeClose(Connection)} the connection.
     */
    @Override
    public void returnConnection(Connection c)
    {
        safeClose(c);
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
        // no synchronisation here needed as producing multiple instances is acceptable
        // and sqlCache is a ConcurrentMap and therefore thread safe
        String cached = sqlCache.get(sql);

        if (cached != null)
        {
            return cached;
        }

        String created = sql.replaceAll("\\{table\\}", getQuotedTable());
        created = created.replaceAll(" ?\\{FOR UPDATE\\}", " FOR UPDATE");

        sqlCache.put(sql, created);

        return created;
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

    /**
     * Create new instance of this dialect with given table name.
     *
     * @see com.seeburger.vfs2.provider.jdbctable.JdbcDialect#cloneDialect(java.lang.String)
     */
    @Override
    public JdbcDialect cloneDialect(String tableName)
    {
        return new JdbcDialectBase(tableName, dataSource);
    }

    /** Close connection and swallow all Exceptions. */
    private static void safeClose(Connection con)
    {
        if (con != null)
        {
            try { con.close(); } catch (Exception ignored) { /* nothing to recover */ }
        }
    }
}
