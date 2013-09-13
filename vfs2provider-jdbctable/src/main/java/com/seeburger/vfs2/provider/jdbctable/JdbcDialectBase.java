/*
 * JdbcDialectBase.java
 *
 * created at 13.09.2013 by Eckenfel <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;


public class JdbcDialectBase implements JdbcDialect
{
    static final int MS = 1000000;
    String tableName;
    DataSource dataSource;
    protected ConcurrentMap<String, String> sqlCache = new ConcurrentHashMap<String, String>(23);

    public JdbcDialectBase(String tableName, DataSource dataSource)
    {
        this.tableName = tableName;
        this.dataSource = dataSource;
    }

    public String getQuotedTable()
    {
        return tableName;
    }

    public Connection getConnection() throws SQLException
    {
        long start = System.nanoTime();
        Connection c = dataSource.getConnection();
        long duration = System.nanoTime() - start;
        if (duration > 100*MS)
            System.out.printf("slow getConnection(): %.6fs%n",  (duration/1000000000.0));
        return c;
    }

    public void returnConnection(Connection c)
    {
        try { c.close(); } catch (Exception ignored) { }
    }

    public String expandSQL(String sql)
    {
        String cached = sqlCache.get(sql);

        if (cached != null)
            return cached;

        synchronized(this)
        {
            String created = sql.replaceAll("\\{table\\}", getQuotedTable());
            created = created.replaceAll(" ?\\{FOR UPDATE\\}", " FOR UPDATE");
            sqlCache.put(sql, created);
            return created;
        }
    }

    public PreparedStatement prepareQuery(Connection con, String sql) throws SQLException
    {
        String expanded = expandSQL(sql);
        return con.prepareStatement(expanded); // TYPE_FORWARD_ONLY, CONCUR_READ_ONLY
    }

    public PreparedStatement prepareUpdateable(Connection con, String sql) throws SQLException
    {
        String expanded = expandSQL(sql);
        return con.prepareStatement(expanded, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
    }

    public boolean supportsAppendBlob()
    {
        return false;
    }

}



