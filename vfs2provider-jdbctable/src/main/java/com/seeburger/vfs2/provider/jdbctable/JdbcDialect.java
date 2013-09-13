/*
 * JdbcDialect.java
 *
 * created at 13.09.2013 by Eckenfel <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


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

    String expandSQL(String sql);

    /**
     * First {@link #expandSQL(String)} then create a prepared statement for query.
     * @throws SQLException
     */
    PreparedStatement prepareQuery(Connection con, String sql) throws SQLException;

    /**
     * First {@link #expandSQL(String)} then create a prepared statement for RS updates.
     * @throws SQLException
     */
    PreparedStatement prepareUpdateable(Connection con, String sql) throws SQLException;

    boolean supportsAppendBlob();

}



