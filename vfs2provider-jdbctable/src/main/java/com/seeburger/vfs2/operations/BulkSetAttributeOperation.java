/*
 * BulkSetAttributeOperation.java
 *
 * created at 2013-11-07 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.operations;


import java.util.Collection;

import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.operations.FileOperation;


/**
 * FileOperation which allows to set an attribute on multiple files.
 * <P>
 * This is typically implemented by file systems with transaction support
 * or generally to reduce the cache overhead of VFS2 for bulk change
 * operations.
 * <P>
 * Future improvement: inherit from a general BulkOperation interface for
 * {@link #setFilesList(Collection)} and {@link #getMissingFiles()}.
 */
public interface BulkSetAttributeOperation extends FileOperation
{
    /**
     * Collection of file names to set the attribute.
     * <P>
     * Names are URIs resolved relative to the FileObject the operation is called on with
     * {@link NameScope#FILE_SYSTEM} (i.e. can be absolute within the same file system).
     */
    void setFilesList(Collection<String> filesList);

    /** Name and Value of Attribute to set on all files. */
    void setAttribute(String attributeName, Object attributeValue);

    /** List of files which have not been found after {@link #process()}. Never null. */
    Collection<String> getMissingFiles();
}
