/*
 * JdbcFileUtil.java
 *
 * created at 2013-09-25 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.impl.DecoratedFileObject;


/** Utility functions to work with {@link FileObject} and {@link JdbcTableRowFile}. */
public class JdbcFileUtil
{
    /** Unwrap decorated fileObject and cast if possible.
     *
     * @param file a possibly decorate {@link JdbcTableRowFile}
     * @return the unwraped obejct or null
     */
    public static JdbcTableRowFile unwrapJdbcFile(FileObject file)
    {
        if (file == null)
        {
            return null;
        }
        FileObject current = file;
        for(int i=0;i<10;i++)
        {
            if (current instanceof JdbcTableRowFile)
            {
                return (JdbcTableRowFile)current;
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
}

