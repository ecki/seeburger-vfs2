/*
 * DarcFileOperationProvider.java
 *
 * created at 2013-10-29 by y.gologanov <y.gologanov@seeburger.com>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.operations.AbstractFileOperationProvider;
import org.apache.commons.vfs2.operations.FileOperation;

import com.seeburger.vfs2.operations.CollectFilesOperation;


/**
 * Provide Darc file system specific file operations.
 */
public class DarcFileOperationProvider extends AbstractFileOperationProvider
{
    public static DarcFileOperationProvider getInstance() throws FileSystemException
    {
        DarcFileOperationProvider provider = new DarcFileOperationProvider();
        provider.addOperation(CollectFilesOperation.class);
        return provider;
    }

    @Override
    protected void doCollectOperations(Collection<Class< ? extends FileOperation>> availableOperations,
                                       Collection<Class< ? extends FileOperation>> resultList,
                                       FileObject file)
        throws FileSystemException
    {
        if (file instanceof DarcFileObject)
        {
            resultList.addAll(availableOperations);
        }
    }

    @Override
    protected FileOperation instantiateOperation(FileObject file,
                                                 Class< ? extends FileOperation> operationClass)
        throws FileSystemException
    {
        if (operationClass == CollectFilesOperation.class)
        {
            return new DarcFileCollectOperation(file);
        }
        else
        {
            throw new FileSystemException("Unsupported operation: " + operationClass);
        }
    }
}



