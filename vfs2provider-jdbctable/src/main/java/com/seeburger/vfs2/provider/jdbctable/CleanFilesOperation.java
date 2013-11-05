/*
 * CleanFilesOperation.java
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
import org.apache.commons.vfs2.operations.FileOperation;

/**
 * This class is a FileOperation implementation responsible for
 * deleting files that have not been marked since a specified time
 *
 * @author y.gologanov
 */
public class CleanFilesOperation implements FileOperation
{

    private FileObject root = null;
    private long timeBefore = 0;

    public CleanFilesOperation(FileObject root)
    {
        this.root = root;
    }

    public void setTimeBefore(long timeBefore)
    {
        this.timeBefore = timeBefore;
    }

    @Override
    public void process()
        throws FileSystemException
    {
        JdbcTableFileSystem fileSystem = (JdbcTableFileSystem) root.getFileSystem();
        JdbcDialect dialect = fileSystem.getDialect();

        Connection connection = null;
        try
        {
            connection = dialect.getConnection();
            PreparedStatement ps = dialect.prepareQuery(connection, "DELETE FROM {table} WHERE cMarkGarbage < ? AND cParent LIKE '/resrepo%'");
            ps.setLong(1, timeBefore);
            ps.executeUpdate();
            connection.commit();
        }
        catch (SQLException e)
        {
            throw new FileSystemException(e);
        }
        finally
        {
            if (connection != null)
            {
                try
                {
                    connection.close();
                }
                catch (SQLException e)
                {
                    //ignore
                }
            }
        }
    }

}



