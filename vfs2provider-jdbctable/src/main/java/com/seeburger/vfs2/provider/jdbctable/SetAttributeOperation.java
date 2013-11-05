/*
 * SetAttributeOperation.java
 *
 * created at 2013-10-29 by y.gologanov <y.gologanov@seeburger.com>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;

import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.operations.FileOperation;

/**
 * This class is a FileOperation implementation responsible for
 * setting an attribute value for a list of specified files.
 *
 * @author y.gologanov
 */
public class SetAttributeOperation implements FileOperation
{

    private FileSystemManager fileSystemManager = null;

    private Collection<String> filesList = null;
    private String attributeName = null;
    private Object attributeValue = null;


    public SetAttributeOperation(FileObject root)
    {
        this.fileSystemManager = root.getFileSystem().getFileSystemManager();
    }

    public void setFilesList(Collection<String> filesList)
    {
        this.filesList = filesList;
    }

    public void setAttributeName(String attributeName)
    {
        this.attributeName = attributeName;
    }

    public void setAttributeValue(Object attributeValue)
    {
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
           FileObject nextFileObject = fileSystemManager.resolveFile(nextFileUri);
           if (nextFileObject != null)
           {
               nextFileObject.getContent().setAttribute(attributeName, attributeValue);
           }
        }
    }

}



