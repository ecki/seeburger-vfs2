/*
 * ExpireFilesOperation.java
 *
 * created at 2013-11-06 by Bernd Eckenfels <b.eckenfels@seeburgr.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.operations;


import org.apache.commons.vfs2.operations.FileOperation;

import com.seeburger.vfs2.provider.jdbctable.JdbcTableOperationProvider;


/**
 * FileOperation responsible for deleting files that have not been
 * marked since a specified time.
 */
public interface ExpireFilesOperation extends FileOperation
{
    /**
     * Specify the cut-off time stamp. All files older than this time stamp will be purged.
     * <P>
     * It is expected by the {@link JdbcTableOperationProvider} that the time stamp is set
     * with <code>setAttribute("markTime", Long.valueOf(time))</code>.
     */
    void setTimeBefore(long timeBefore);

    /**
     * Set the directory name (prefix) which should be considered.
     * <P>
     * This expects the last component to be a complete directory name, trailing slash is optional.
     * <P>
     * If this is not set, or set to null, all files are considered.
     */
    void setParentPrefix(String name);

    /** Query number of deleted files after {@link #process()} was called. */
    int getDeleteCount();
}
