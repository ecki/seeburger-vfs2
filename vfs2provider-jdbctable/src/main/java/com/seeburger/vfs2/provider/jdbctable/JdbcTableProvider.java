/*
 * JdbcTableProvider.java
 *
 * created at 2013-08-09 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.sql.DataSource;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;


/** Provide a new Apache VFS2 file system to access Blobs in database via JDBC. */
public class JdbcTableProvider
    extends AbstractOriginatingFileProvider
{
    static final Collection<Capability> capabilities = Collections.unmodifiableCollection(Arrays.asList(new Capability[]
    {
        Capability.GET_TYPE,
        Capability.READ_CONTENT,
        Capability.LAST_MODIFIED,
        Capability.FS_ATTRIBUTES,
        Capability.URI,
        Capability.GET_LAST_MODIFIED,
        Capability.ATTRIBUTES,
        //Capability.RANDOM_ACCESS_READ,
        Capability.LIST_CHILDREN,

        Capability.CREATE,
        Capability.WRITE_CONTENT,
        //Capability.RANDOM_ACCESS_WRITE,
        Capability.APPEND_CONTENT,
        Capability.DELETE,
        Capability.RENAME
    }));

    static final Collection<Capability> writeCapabilities = Collections.unmodifiableCollection(Arrays.asList(new Capability[]
    {
        Capability.CREATE,
        Capability.WRITE_CONTENT,
        Capability.RANDOM_ACCESS_WRITE,
        Capability.APPEND_CONTENT,
        Capability.DELETE,
        Capability.RENAME
    }));

    /** The specified dialect template (will be cloned for different table names) */
    final JdbcDialect dialect;

    // TODO: this needs a JdbcTableProvider(FileSystemManager manager) constructor for file config

	/**
     * Constructs a new provider for given DataSource.
     *
	 * @throws FileSystemException
	 *
	 * @see JdbcDialectBase#getDialect(DataSource)
     */
    public JdbcTableProvider(DataSource dataSource) throws FileSystemException
    {
        super();
        try
        {
            this.dialect = JdbcDialectBase.getDialect(dataSource);
        }
        catch (SQLException e)
        {
            throw new FileSystemException("Exception while obtaining database's metadata!", e);
        }

        // super does setFileNameParser(GenericFileNameParser.getInstance());
    }

    /**
     * Constructs a new provider for given Dialect.
     */
    public JdbcTableProvider(JdbcDialect dialect)
    {
        super();
        this.dialect = dialect;
        // super does setFileNameParser(GenericFileNameParser.getInstance());
    }


    @Override
    protected FileSystem doCreateFileSystem(final FileName name, final FileSystemOptions fileSystemOptions)
        throws FileSystemException
    {
        return new JdbcTableFileSystem(name, this, fileSystemOptions);
    }

    @Override
    public FileSystemConfigBuilder getConfigBuilder()
    {
        return JdbcTableFileSystemConfigBuilder.getInstance();
    }

    public Collection<Capability> getCapabilities()
    {
        return capabilities;
    }

    /** Return a new Dialect instance and configure it for the given table name. */
    protected JdbcDialect getDialectForTable(String tablename)
    {
        return dialect.cloneDialect(tablename);
    }
}
