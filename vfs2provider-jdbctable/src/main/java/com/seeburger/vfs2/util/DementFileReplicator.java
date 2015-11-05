/*
 * DementFileReplicator.java
 *
 * created at 2015-11-05 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.util;


import java.io.File;

import org.apache.commons.vfs2.impl.DefaultFileReplicator;


/**
 * This modified FileReplicator does not remeber the
 * replicated files in order to avoid a leak.
 */
public class DementFileReplicator extends DefaultFileReplicator
{
    /** Create replicator with default temp directory. */
    public DementFileReplicator()
    {
        super();
    }

    /** Create replicator with default temp directory. */
    public DementFileReplicator(final File tempDir)
    {
        super(tempDir);
    }

    @Override
    public void close()
    {
        /* does nothing, no files remebered to delete. */
    }

    @Override
    protected void addFile(Object file)
    {
        /* does nothing, no files remebered to delete. */
    }
}
