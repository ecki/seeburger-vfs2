package com.seeburger.vfs2.provider.jdbctable;


import java.sql.Connection;
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
        Capability.CREATE,
        Capability.GET_TYPE,
        Capability.READ_CONTENT,
        Capability.WRITE_CONTENT,
        Capability.LAST_MODIFIED,
        Capability.FS_ATTRIBUTES,
        Capability.URI,
        Capability.GET_LAST_MODIFIED,
        Capability.ATTRIBUTES,
        //Capability.RANDOM_ACCESS_READ,
        //Capability.RANDOM_ACCESS_WRITE,
        Capability.LIST_CHILDREN,
        Capability.APPEND_CONTENT, // conditional
        Capability.DELETE,         // conditional
        Capability.RENAME          // conditional
    }));

	DataSource dataSource;
    boolean supportsAppendBlob;


	/**
     * Constructs a new provider for given DataSource.
     */
    public JdbcTableProvider(DataSource dataSource)
    {
        super();
        setFileNameParser(JdbcTableNameParser.getInstance());
        this.dataSource = dataSource;
        this.supportsAppendBlob = supportsAppendBlob(dataSource); // TODO: dialect
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

    public boolean supportsAppendBlob()
    {
        return supportsAppendBlob;
    }

    public boolean supportsAppendBlob(DataSource ds)
    {
        Connection c = null;
        try
        {
            c = ds.getConnection();
            String product = c.getMetaData().getDatabaseProductName();
            if (product.contains("H2"))
                return false;
            else
                return true;
        }
        catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return true;
        }
        finally
        {
            safeClose(c);
        }
    }

    private void safeClose(Connection c)
    {
        try
        {
            c.close();
        } catch (Exception ignored) { }
    }
}
