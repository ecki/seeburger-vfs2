/*
 * JdbcTableOperationProvider.java
 *
 * created at 2013-10-29 by y.gologanov <y.gologanov@seeburger.com>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;

import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.operations.AbstractFileOperationProvider;
import org.apache.commons.vfs2.operations.FileOperation;

import com.seeburger.vfs2.operations.BulkSetAttributeOperation;
import com.seeburger.vfs2.operations.ExpireFilesOperation;

/**
 * This class is an implementation of AbstractFileOperationProvider
 * that provides JDBC file system specific operations.
 */
public class JdbcTableOperationProvider extends AbstractFileOperationProvider
{

    /**
    *
    * @return A new instance of the class
    * @throws FileSystemException
    */
    public static JdbcTableOperationProvider getInstance() throws FileSystemException
    {
        JdbcTableOperationProvider provider = new JdbcTableOperationProvider();
        provider.addOperation(ExpireFilesOperation.class);
        provider.addOperation(BulkSetAttributeOperation.class);
        return provider;
    }

    @Override
    protected void doCollectOperations(Collection<Class< ? extends FileOperation>> availableOperations,
                                       Collection<Class< ? extends FileOperation>> resultList,
                                       FileObject file)
        throws FileSystemException
    {
        if (file instanceof JdbcTableRowFile)
        {
            resultList.addAll(availableOperations);
        }
    }

    @Override
    protected FileOperation instantiateOperation(FileObject file, Class< ? extends FileOperation> operationClass)
        throws FileSystemException
    {
        if (operationClass == ExpireFilesOperation.class)
        {
            return new JdbcFileExpireOperation(file);
        }
        else if (operationClass == BulkSetAttributeOperation.class)
        {
            return new JdbcFileBulkSetOperation(file);
        }
        else
        {
            throw new FileSystemException("vfs.operation/not-found.error", operationClass);
        }
    }
}



