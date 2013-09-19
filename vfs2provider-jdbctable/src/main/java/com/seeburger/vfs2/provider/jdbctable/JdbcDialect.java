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


/**
 * Database and Driver specific functions.
 * <P>
 * This can be used to customize the way connections are retrieved and used.
 */
public interface JdbcDialect
{
    /** Returns the (appropriately quoted) table identifier for use in Statements. */
    String getQuotedTable();

    /** Get connection (from DataSource).
     * @throws SQLException
     */
    Connection getConnection() throws SQLException;

    /** Connection is no longer needed (aka close() it). */
    void returnConnection(Connection connection);

    /** replace placehodlers like {{table}} and {{FOR UPDATE}} placeholders. Expansions can be cached. */
    String expandSQL(String sql);

    /**
     * First {@link #expandSQL(String)} then create a prepared statement for query.
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

    boolean supportsAppendBlob();
}
