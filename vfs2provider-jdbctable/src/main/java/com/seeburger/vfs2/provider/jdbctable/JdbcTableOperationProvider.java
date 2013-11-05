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

/**
*
* This class is an implementation of AbstractFileOperationProvider
* that provides JDBC file system specific operations.
*
* @author y.gologanov
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
        provider.addOperation(CleanFilesOperation.class);
        provider.addOperation(SetAttributeOperation.class);
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
    protected FileOperation instantiateOperation(FileObject file,
                                                 Class< ? extends FileOperation> operationClass)
        throws FileSystemException
    {
        if (operationClass == CleanFilesOperation.class)
        {
            return new CleanFilesOperation(file);
        }
        else if (operationClass == SetAttributeOperation.class)
        {
            return new SetAttributeOperation(file);
        }
        else
        {
            throw new FileSystemException("Unsupported operation: " + operationClass);
        }
    }

}



