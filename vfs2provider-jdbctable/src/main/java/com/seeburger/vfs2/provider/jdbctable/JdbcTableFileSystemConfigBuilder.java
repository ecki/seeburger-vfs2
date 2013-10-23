/*
 * JdbcTableFileSystemConfigBuilder.java
 *
 * created at 2013-08-09 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;


/**
 * Configuration options for SEEBURGER Jdbc Table.
 * <P>
 * For the JDBC Table file system you can configure the name of the table to use
 * or if you want to use it read-only or read-write.
 *
 * @see #setTablename(FileSystemOptions, String)
 * @see #setWriteMode(FileSystemOptions, boolean)
 */
public class JdbcTableFileSystemConfigBuilder extends FileSystemConfigBuilder
{
    /** Singleton of this builder. */
    private static final JdbcTableFileSystemConfigBuilder BUILDER = new JdbcTableFileSystemConfigBuilder();

    protected JdbcTableFileSystemConfigBuilder(final String prefix)
    {
        super(prefix);
    }

    private JdbcTableFileSystemConfigBuilder()
    {
        super("seejt.");
    }

    public static JdbcTableFileSystemConfigBuilder getInstance()
    {
        return BUILDER;
    }

    /** Specify a new table name. */
    public void setTablename(final FileSystemOptions opts, final String name)
    {
        setParam(opts, "table", name);
    }

    /** What is the table name to store data (Default: {@value JdbcDialectBase#TABLE_NAME}. */
    public String getTablename(final FileSystemOptions opts)
    {
        return getString(opts, "table", JdbcDialectBase.TABLE_NAME);
    }

    /** Set a new write mode. */
    public void setWriteMode(final FileSystemOptions opts, final boolean write)
    {
        setParam(opts, "write", Boolean.valueOf(write));
    }

    /** Does the filesystem allow write access (Default: true). */
    public boolean getWriteMode(final FileSystemOptions opts)
    {
        return getBoolean(opts, "write", true);
    }

    @Override
    protected Class<? extends FileSystem> getConfigClass()
    {
        return JdbcTableFileSystem.class;
    }
}
