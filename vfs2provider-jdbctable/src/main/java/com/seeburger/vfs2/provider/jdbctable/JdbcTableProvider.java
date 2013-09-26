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

    static final int MS = 1000000;

    final JdbcDialect dialect;

    // TODO: this needs a JdbcTableProvider(FileSystemManager manager) constructor for file config

	/**
     * Constructs a new provider for given DataSource.
	 * @throws FileSystemException
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

        setFileNameParser(JdbcTableNameParser.getInstance());
    }

    /**
     * Constructs a new provider for given Dialect.
     */
    public JdbcTableProvider(JdbcDialect dialect)
    {
        super();
        this.dialect = dialect;
        setFileNameParser(JdbcTableNameParser.getInstance());
    }

    @Override
    protected FileSystem doCreateFileSystem(final FileName name, final FileSystemOptions fileSystemOptions)
        throws FileSystemException
    {
        // name can be instance of GenericFileName or URLFileName
        //final URLFileName rootName = (UrlFileNameParser) name;
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
    public JdbcDialect getDialectForTable(String tablename)
    {
        return dialect.getDialectForTable(tablename);
    }

}
