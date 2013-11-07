/*
 * CollectFilesOperation.java
 *
 * created at 2013-10-29 by y.gologanov <y.gologanov@seeburger.com>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.operations;


import java.util.Collection;

import org.apache.commons.vfs2.operations.FileOperation;


/**
 * FileOperation to collect referenced files.
 */
public interface CollectFilesOperation extends FileOperation
{
    /**
     * @param filesList a Collection where discovered files to be added to.
     */
    public void setFilesList(Collection<String> filesList);
}
