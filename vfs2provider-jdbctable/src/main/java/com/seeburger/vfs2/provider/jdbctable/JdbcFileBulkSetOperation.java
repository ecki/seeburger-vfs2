/*
 * JdbcFileBulkSetOperation.java
 *
 * created at 2013-10-29 by y.gologanov <y.gologanov@seeburger.com>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.NameScope;

import com.seeburger.vfs2.operations.BulkSetAttributeOperation;


/**
 * FileOperation implementation responsible for bulk setting attributes.
 */
public class JdbcFileBulkSetOperation implements BulkSetAttributeOperation
{
    private final FileObject root;
    private Collection<String> filesList = null;
    private String attributeName = null;
    private Object attributeValue = null;
    private Collection<String> missingFiles = new ArrayList<String>();


    public JdbcFileBulkSetOperation(FileObject root)
    {
        this.root = root;
    }

    @Override
    public void setFilesList(Collection<String> filesList)
    {
        this.filesList = filesList;
    }

    @Override
    public Collection<String> getMissingFiles()
    {
        return missingFiles;
    }


    @Override
    public void setAttribute(String attributeName, Object attributeValue)
    {
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }


    @Override
    public void process()
        throws FileSystemException
    {
        if (filesList == null)
        {
            return;
        }

        for (String nextFileUri : filesList)
        {
           FileObject nextFileObject = root.resolveFile(nextFileUri, NameScope.FILE_SYSTEM);
           if (nextFileObject == null || !nextFileObject.exists())
           {
               missingFiles.add(nextFileUri);
               continue;
           }

           // TODO: use JDBC with a single transaction instead.
           nextFileObject.getContent().setAttribute(attributeName, attributeValue);
        }
    }



}



