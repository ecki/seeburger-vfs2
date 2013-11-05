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

import com.seeburger.vfs2.provider.digestarc.DarcFileObject;

/**
 *
 * This class is an implementation of AbstractFileOperationProvider
 * that provides Darc file system specific operations.
 *
 * @author y.gologanov
 */
public class DarcFileOperationProvider extends AbstractFileOperationProvider
{

    /**
     *
     * @return A new instance of the class
     * @throws FileSystemException
     */
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
            return new CollectFilesOperation(file);
        }
        else
        {
            throw new FileSystemException("Unsupported operation: " + operationClass);
        }
    }

}



