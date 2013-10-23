/*
 * JdbcTableFileSystem.java
 *
 * created at 2013-08-09 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;


/**
 * An Apache VFS2 file system backed by a JDBC Table.
 */
public class JdbcTableFileSystem extends AbstractFileSystem
{
    /** dialect instance used to communicate with database driver. */
    final JdbcDialect dialect;
    /** file system provider creating this instance. */
    final JdbcTableProvider provider;
    /** Is write access allowed? */
    final boolean writeMode;


    protected JdbcTableFileSystem(final FileName rootName,
                                  final JdbcTableProvider jdbcTableProvider,
                                  final FileSystemOptions fileSystemOptions)
    {
        super(rootName, /*parentlayer*/null, fileSystemOptions);
        this.provider = jdbcTableProvider;

        // extract configuration options
        JdbcTableFileSystemConfigBuilder config = JdbcTableFileSystemConfigBuilder.getInstance();
        this.writeMode = config.getWriteMode(fileSystemOptions);
        this.dialect = jdbcTableProvider.getDialectForTable(config.getTablename(fileSystemOptions));
    }

    /**
     * Adds the capabilities of this file system.
     */
    @Override
    protected void addCapabilities(final Collection<Capability> caps)
    {
        caps.addAll(JdbcTableProvider.capabilities);
        // do we have the capability to change things?
        if (!writeMode)
        {
            caps.removeAll(JdbcTableProvider.writeCapabilities);
        }
    }

    /**
     * Creates a file object.
     * <P>
     * This method is called only if the requested file is not cached.
     *
     * @see JdbcTableRowFile
     */
    @Override
    protected FileObject createFile(final AbstractFileName name)
                    throws Exception
    {
        return new JdbcTableRowFile(name, this);
    }

    /**
     * Access to the databse for {@link JdbcTableRowFile}.
     *
     * @see JdbcTableRowFile
     */
    protected JdbcDialect getDialect()
    {
        return dialect;
    }
}
