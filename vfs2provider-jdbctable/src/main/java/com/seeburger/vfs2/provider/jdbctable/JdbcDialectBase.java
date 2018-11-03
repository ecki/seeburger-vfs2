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
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLTransientException;
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
 * @see JdbcDialectPostgreSQL
 */
public class JdbcDialectBase implements JdbcDialect
{
    private static final String ORACLE_DIALECT = "oracle";
    private static final String MSSQL_DIALECT$ = "micro$oft";
    private static final String MSSQL_DIALECT = "microsoft";
    private static final String POSTGRESQL_DIALECT = "postgresql";
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
        SQLException lastEx = null;
        for(int retry=0; retry < 2; retry++)
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
                else if (provider.contains(POSTGRESQL_DIALECT))
                {
                    return new JdbcDialectPostgreSQL(ds);
                }
                else
                {
                    return new JdbcDialectBase(ds);
                }
            }
            catch(SQLException ex )
            {
                lastEx = ex;
            }
            finally
            {
                safeClose(con);
            }
        } // retry
        throw lastEx;
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
        catch (RuntimeException unexpected) // for example Oracle NegativeArrayExcepion
        {
            throw new SQLTransientConnectionException("Problem while getting connection. RuntimeException=" + unexpected, unexpected);
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

    @Override
    public String getQuotedTable()
    {
        return tableName;
    }

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

    @Override
    public PreparedStatement prepareQuery(Connection con, String sql)
        throws SQLException
    {
        String expanded = expandSQL(sql);
        return con.prepareStatement(expanded); // TYPE_FORWARD_ONLY, CONCUR_READ_ONLY
    }

    @Override
    public PreparedStatement prepareUpdateable(Connection con, String sql)
        throws SQLException
    {
        String expanded = expandSQL(sql);
        return con.prepareStatement(expanded, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
    }

    @Override
    public boolean supportsAppendBlob()
    {
        return false;
    }

    @Override
    public boolean supportsBlob()
    {
        return true;
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

    @Override
    public boolean shouldRetry(Exception e, int retry)
    {
        // first occurrence of any error is retried
        if (retry == 0)
            return true;

        // We no more than 3 attempts
        if (retry > 2 || retry < 0) // negative overflow should not happen
            return false;

        if (e instanceof SQLTransientException ||
            e instanceof SQLTransientConnectionException ||
            e instanceof SQLRecoverableException)
        {
            return true; // endless retries are avoided by #maxRetry()
        }

        // some drivers might not use above subexceptions, lets look at the state:
        if (e instanceof SQLException)
        {
            SQLException sqlException = (SQLException)e;
            String sqlState = sqlException.getSQLState();
            if (sqlState != null && sqlState.startsWith("08"))
            {
                return true;
            }
        }

        // not recognized as a retryable db problem
        return false;
    }
}
