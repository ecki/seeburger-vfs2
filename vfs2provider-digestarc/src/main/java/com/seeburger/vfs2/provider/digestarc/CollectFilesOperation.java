/*
 * CollectFilesOperation.java
 *
 * created at 2013-10-29 by y.gologanov <y.gologanov@seeburger.com>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.operations.FileOperation;
import org.apache.commons.vfs2.operations.FileOperations;

/**
 * This class is a FileOperation implementation responsible for
 * creating a list of file uri string object from a specified file tree.
 *
 * @author y.gologanov
 */
public class CollectFilesOperation
    implements FileOperation
{

    private FileObject root = null;
    private Collection<String> filesList = null;

    /**
     *
     * @param filesList a Collection where discovered files to be added to.
     */
    public void setFilesList(Collection<String> filesList)
    {
        this.filesList = filesList;
    }

    /**
     * @param root A file tree root object to be iterated
     */
    public CollectFilesOperation(FileObject root)
    {
        this.root = root;
    }

    @Override
    public void process()
        throws FileSystemException
    {
        if (root instanceof DarcFileObject)
        {
            DarcFileObject darcFile = (DarcFileObject) root;
            FileObject delegateFile = darcFile.getDelegateFile();

            filesList.add(delegateFile.getName().getURI());

            if (darcFile.getType() == FileType.FOLDER)
            {
                FileObject[] fileList = darcFile.getChildren();
                for (FileObject nextFile : fileList)
                {
                    FileOperations fileOperations = nextFile.getFileOperations();
                    CollectFilesOperation operation = (CollectFilesOperation) fileOperations.getOperation(CollectFilesOperation.class);
                    if (operation != null)
                    {
                        operation.setFilesList(filesList);
                        operation.process();
                    }
                }
            }
        }
    }

}
