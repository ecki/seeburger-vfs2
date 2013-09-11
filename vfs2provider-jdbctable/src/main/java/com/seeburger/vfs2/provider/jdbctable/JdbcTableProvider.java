package com.seeburger.vfs2.provider.jdbctable;


import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
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
        }
        catch (Exception ignored) { }
    }

    Connection getConnection() throws SQLException
    {
        long start = System.nanoTime();
        Connection c = dataSource.getConnection();
        long duration = System.nanoTime() - start;
        if (duration > 600*1000000)
            System.out.printf("slow getConnection(): %.6fs%n",  (duration/1000000000.0));
        return c;
    }

    public void closeConnection(Blob blob, ResultSet rs, PreparedStatement ps, Connection connection)
    {
        if (blob != null)
        {
            try
            {
                blob.free();
            }
            catch (AbstractMethodError ignored) { } // TODO: JTDS
            catch (Exception ignored) { }
        }

        if (rs != null)
        {
            processWarnings(rs);
            try
            {
                rs.close();
            }
            catch (Exception ignored) { }
        }

        if (ps != null)
        {
            processWarnings(ps);
            try
            {
                ps.close();
            }
            catch (Exception ignored) { }
        }

        if (connection != null)
        {
            processWarnings(connection);

            try
            {
                connection.close();
            }
            catch (Exception ignored) { }
        }
    }

    private void processWarnings(ResultSet rs)
    {
        try
        {
            processWarnings(rs.getWarnings());
        }
        catch (SQLException ignored) { }
    }

    private void processWarnings(Connection connection)
    {
        try
        {
            processWarnings(connection.getWarnings());
        }
        catch (SQLException ignored) { }
    }

    private void processWarnings(PreparedStatement ps)
    {
        try
        {
            processWarnings(ps.getWarnings());
        }
        catch (SQLException ignored) { }
    }

    private void processWarnings(SQLWarning warnings)
    {
        if (warnings != null)
        {
            RuntimeException stack = new RuntimeException("Found JDBC Warnings: " + warnings);
            stack.fillInStackTrace();
            stack.printStackTrace(System.err);
        }
    }

    public void rollbackConnection(Blob blob, ResultSet rs, PreparedStatement ps, Connection connection)
    {
        closeConnection(blob, rs, ps, null);

        if (connection != null)
        {
            processWarnings(connection);

            try
            {
                connection.rollback();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            try
            {
                connection.close();
            }
            catch (Exception ignored) { }
        }
    }
}
