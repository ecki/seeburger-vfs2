/*
 * DarcFileUtil.java
 *
 * created at 2013-09-24 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved. TODO
 */
package com.seeburger.vfs2.provider.digestarc;


import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DecoratedFileObject;


public class DarcFileUtil
{
    /** No need to instantiate this class, private constructor. */
    private DarcFileUtil() { }

    /**
     * Cast or unwrap FileObject to provider specific implementation.
     * <P>
     * Can deal with decorated file objects.
     * @param file
     * @return
     */
    public static DarcFileObject unwrapDarcFile(FileObject file)
    {
        if (file == null)
        {
            return null;
        }
        FileObject current = file;
        for(int i=0;i<10;i++)
        {
            if (current instanceof DarcFileObject)
            {
                return (DarcFileObject)current;
            }
            if (current instanceof DecoratedFileObject)
            {
                current = ((DecoratedFileObject)current).getDecoratedFileObject();
                // if null falls through next two instanceofs.
            }
            else
            {
                return null;
            }
        }
        return null;
    }

    /**
     * Extract the hash (blob or tree-id) from a given FileObject.
     *
     * @throws FileSystemException
     */
    public static String getDarcFileHash(FileObject file) throws FileSystemException
    {
        DarcFileObject darcFile = unwrapDarcFile(file);
        if (darcFile != null)
        {
            Object att = darcFile.getContent().getAttribute(DarcFileObject.ATTRIBUTE_GITHASH);
            if (att instanceof String)
            {
                return (String)att;
            }
        }
        // TODO: fallback
        return null;
    }


    /**
     * Commit the changes to the filesystem owning the specified file and return new treeID.
     *
     * @param file any file inside a DarcFileSystem
     * @return the new commitID or null if no changes
     * @throws IOException if the commit operation failed the exception is forwarded
     * @throws IllegalArgumentException if the specified file cannot be casted to a DarcFileSystem
     */
    public static String commitChanges(FileObject file) throws IOException
    {
        DarcFileObject darcFile = unwrapDarcFile(file);
        if (darcFile == null)
        {
            throw new IllegalArgumentException("For commiting the digest archive you need to specify a DarcFileObject: " + file);
        }

        FileSystem fs = darcFile.getFileSystem();
        if (!(fs instanceof DarcFileSystem))
        {
            throw new IllegalArgumentException("The class=" + fs.getClass() + " is no DarcFileSystem for " + file);
        }

        DarcFileSystem dfs = (DarcFileSystem)fs;
        return dfs.commitChanges();
    }


    /**
     * Return the digest of the specified object.
     *
     * @param file and FileObject within a digest archive filesystem
     * @return string if it has the githash attribute or null
     * @throws FileSystemException problems while accessing the file are rethrown
     */
    public static String getObjectID(FileObject file) throws FileSystemException
    {
        return (String)file.getContent().getAttribute(DarcFileObject.ATTRIBUTE_GITHASH);
    }
}
