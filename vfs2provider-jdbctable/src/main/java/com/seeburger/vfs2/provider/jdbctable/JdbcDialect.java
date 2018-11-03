/*
 * JdbcDialect.java
 *
 * created at 2013-09-13 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;


/**
 * Database driver abstraction layer.
 * <P>
 * This can be used to customize the way connections are retrieved and used. The default
 * implementations {@link JdbcDialectBase} and {@link JdbcDialectMSSQL} and {@link JdbcDialectOracle}
 * are all based on {@link DataSource} objects.
 */
public interface JdbcDialect
{
    /**
     * Returns the (appropriately quoted) table identifier for use in Statements.
     * <P>
     * This is also used to construct namespaces (schema, owener, database).
     */
    String getQuotedTable();

    /**
     * Get connection (from DataSource).
     * <P>
     * It is expected autocommit is turned off for this connection and it
     * is actually lend from a pool.
     *
     * @throws SQLException
     */
    Connection getConnection() throws SQLException;

    /**
     * Connection is no longer needed.
     * <P>
     * This will be used after each transaction to free/return the connection.
     * In the base implementation this simply calls {@link Connection#close()}.
     * <P>
     * If <code>connection</code> parameter is null this does nothing.
     */
    void returnConnection(Connection connection);

    /** replace place holders like {{table}} and {{FOR UPDATE}}. Expansions might be cached. */
    String expandSQL(String sql);

    /**
     * First {@link #expandSQL(String)} then create a prepared statement for query.
     *
     * @throws SQLException
     */
    PreparedStatement prepareQuery(Connection con, String sql) throws SQLException;

    /**
     * First {@link #expandSQL(String)} then create a prepared statement for {@link ResultSet}
     * updates.
     *
     * @throws SQLException
     */
    PreparedStatement prepareUpdateable(Connection con, String sql) throws SQLException;

    /** Does this dialect allow positional updates to Blobs (with setBytes(int, byte[]) or requires rewrite. */
    boolean supportsAppendBlob();

    /** Does this dialect allow to access Blobs with Blob datatype (instead of byte[]). */
    boolean supportsBlob();

    /**
     * Clone the dialect object with given table name.
     * <P>
     * This is typically used when the file system is created. The table name is read from
     * {@link JdbcTableFileSystemConfigBuilder}.
     *
     * @see JdbcTableProvider#createFileSystem(String, org.apache.commons.vfs2.FileObject, org.apache.commons.vfs2.FileSystemOptions)
     */
    JdbcDialect cloneDialect(String tableName);

    /**
     * Is called when an SQL Exception in a particular retry happened.
     *
     * @param exception the observed exception
     * @param retry retry iteration (first is 0)
     * @return true if the operation should be retried due to transient DB problems
     */
    boolean shouldRetry(Exception exception, int retry);
}
