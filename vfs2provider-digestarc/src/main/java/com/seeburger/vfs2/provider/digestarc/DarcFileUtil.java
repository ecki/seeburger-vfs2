/*
 * DarcFileUtil.java
 *
 * created at 2013-09-24 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved. TODO
 */
package com.seeburger.vfs2.provider.digestarc;


import org.apache.commons.vfs2.FileObject;
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
}
