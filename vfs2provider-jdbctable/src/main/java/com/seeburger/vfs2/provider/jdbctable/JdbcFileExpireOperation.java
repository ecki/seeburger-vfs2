/*
 * JdbcFileExpireOperation.java
 *
 * created at 2013-10-29 by y.gologanov <y.gologanov@seeburger.com>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import com.seeburger.vfs2.operations.ExpireFilesOperation;


/**
 * Implementation for the JDBC table provider to delete untouched files.
 *
 * @see #setParentPrefix(String)
 * @see #setTimeBefore(long)
 * @see #process()
 * @see #getDeleteCount()
 */
public class JdbcFileExpireOperation implements ExpireFilesOperation
{
    private FileObject root = null;
    private long timeBefore = 0;
    private String prefix = null;
    private int deleteCount = -1;

    public JdbcFileExpireOperation(FileObject root)
    {
        this.root = root;
    }

    @Override
    public void setTimeBefore(long timeBefore)
    {
        this.timeBefore = timeBefore;
    }

    @Override
    public void setParentPrefix(String name)
    {
        this.prefix = name;
    }

    @Override
    public int getDeleteCount()
    {
        return this.deleteCount;
    }


    @Override
    public void process()
        throws FileSystemException
    {
        JdbcTableFileSystem fileSystem = (JdbcTableFileSystem) root.getFileSystem();
        JdbcDialect dialect = fileSystem.getDialect();

        Connection connection = null;
        PreparedStatement ps = null;
        try
        {
            // this operation is not retried: application would just retry later
            connection = dialect.getConnection();
            if (prefix != null)
            {
                if (prefix.endsWith("/")) // TODO: prefix==/
                {
                    prefix = prefix.substring(0, prefix.length()-1);
                }
                // size >= 0 makes sure we do not delete directories
                ps = dialect.prepareQuery(connection, "DELETE FROM {table} WHERE cMarkGarbage < ? AND (cParent = ? OR cParent LIKE ?) AND cSize >= 0");
                ps.setLong(1, timeBefore);
                ps.setString(2, prefix);
                ps.setString(3, prefix + "/%"); // TODO: escape
            }
            else
            {
                ps = dialect.prepareQuery(connection, "DELETE FROM {table} WHERE cMarkGarbage < ? AND cSize >= 0");
                ps.setLong(1, timeBefore);
            }
            this.deleteCount = ps.executeUpdate();
            connection.commit();

            // TODO: do we need to flush the caches?
        }
        catch (SQLException e)
        {
            throw new FileSystemException(e);
        }
        finally
        {
            if (ps != null)
            {
                try { ps.close(); } catch (Exception ignored) { /* nothing to do. */ }
            }
            dialect.returnConnection(connection);
        }
    }
}



