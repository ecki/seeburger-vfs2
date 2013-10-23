/*
 * DarcFileConfigBuilder.java
 *
 * created at 2013-09-25 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;


/**
 * Configuration options for SEEBURGER Digest Archive file system.
 * <P>
 * The Darc file system supports specifying a change session. All
 * clients using the same session will see uncommitted changes. Without
 * a change session the file system is read only.
 */
public class DarcFileConfigBuilder extends FileSystemConfigBuilder
{
    private static final DarcFileConfigBuilder BUILDER = new DarcFileConfigBuilder();

    protected DarcFileConfigBuilder(final String prefix)
    {
        super(prefix);
    }

    private DarcFileConfigBuilder()
    {
        super("seedarc.");
    }

    public static DarcFileConfigBuilder getInstance()
    {
        return BUILDER;
    }

    public void setChangeSession(final FileSystemOptions opts, final String name)
    {
        setParam(opts, "session", name);
    }

    public String getChangeSession(final FileSystemOptions opts)
    {
        return getString(opts, "session", null);
    }

    @Override
    protected Class<? extends FileSystem> getConfigClass()
    {
        return DarcFileSystem.class;
    }
}
