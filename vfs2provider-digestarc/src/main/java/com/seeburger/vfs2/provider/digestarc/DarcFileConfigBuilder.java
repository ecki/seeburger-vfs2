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
 * <p>
 * In this case a session can be specified as well, this is used to create
 * multiple different/independend filesystem instances.
 *
 * @see #setChangeSession(FileSystemOptions, String)
 * @see #setReadSession(FileSystemOptions, String)
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

    public void setChangeSession(final FileSystemOptions opts, final String sessionName)
    {
        setParam(opts, "session.change", sessionName);
    }

    public String getChangeSession(final FileSystemOptions opts)
    {
        return getString(opts, "session.change", null);
    }

    public void setReadSession(final FileSystemOptions opts, final String sessionName)
    {
        setParam(opts, "session.read", sessionName);
    }

    public String getReadSession(final FileSystemOptions opts)
    {
        return getString(opts, "session.read", null);
    }

    /**
     * Get a copy of filesystem options without the session information.
     * <p>
     * The instance is used to lookup the underlying file system. This version
     * produces new instances which is not optimal.
     */
    protected FileSystemOptions getCleanClone(final FileSystemOptions opts)
    {
        if (opts == null)
        {
            return null;
        }
        final FileSystemOptions newOpts = (FileSystemOptions)opts.clone();
        setParam(newOpts, "session.read", null);
        setParam(newOpts, "session.change", null);
        return newOpts;
    }

    @Override
    protected Class<? extends FileSystem> getConfigClass()
    {
        return DarcFileSystem.class;
    }
}
