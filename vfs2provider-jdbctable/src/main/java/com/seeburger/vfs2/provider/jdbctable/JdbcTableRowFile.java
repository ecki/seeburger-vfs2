package com.seeburger.vfs2.provider.jdbctable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.util.RandomAccessMode;

public class JdbcTableRowFile extends AbstractFileObject
{
    JdbcTableProvider provider;
    long lastModified = -1;
    long contentSize = -1;

    public JdbcTableRowFile(AbstractFileName name, JdbcTableFileSystem fs)
    {
        super(name, fs);
        provider = fs.provider;
    }

    @Override
    protected void doAttach() throws Exception
    {
        Connection connection = provider.dataSource.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        Blob blob = null;
        try
        {
            ps = connection.prepareStatement("SELECT cName,cLastModified,cBlob FROM tBlobs WHERE (cParent=? AND cName=?) OR (cParent=? AND cName='')");
            setPrimaryKey(ps, this, 0);
            ps.setString(3, getName().getPathDecoded());
            rs = ps.executeQuery();
            if (!rs.next())
            {
                injectType(FileType.IMAGINARY);
                return;
            }
            String name = rs.getString(1);
            lastModified = rs.getLong(2);
            if (name == null || name.isEmpty())
            {
                injectType(FileType.FOLDER);
            }
            else
            {
                blob = rs.getBlob(3);
                if (blob == null)
                {
                    throw new RuntimeException("Critical inconsitency, blob column is null for " +getName());
                }

                contentSize = blob.length();
                injectType(FileType.FILE);
            }


            // TODO: additional attributes

            if (rs.next())
            {
                throw new RuntimeException("Critical consitency problem, duplicate response to " + getName());
            }
        }
        finally
        {
            safeFree(blob);
            safeClose(rs);
            safeClose(ps);
            safeClose(connection);
        }
    }

    @Override
    protected void doCreateFolder()
        throws Exception
    {
        long now = System.currentTimeMillis(); // TODO: DB side?
        Connection connection = provider.dataSource.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            ps = connection.prepareStatement("INSERT INTO tBlobs (cParent,cName,cSize,cLastModified,cMarkGarbage) VALUES (?,'',-1,?,?)");
            ps.setString(1, getName().getPathDecoded());
            ps.setLong(2, now);
            ps.setLong(3, now);

            int count = ps.executeUpdate();
            if (count != 1)
            {
                throw new RuntimeException("Inserting different than 1 (" + count + ") records for " + getName());
            }

            connection.commit();
            connection = null;

            lastModified = now;
            contentSize = -1;

            injectType(FileType.FOLDER); // TODO: attached?
        }
        finally
        {
            if (connection != null)
            {
                connection.rollback();
            }
            safeClose(rs);
            safeClose(ps);
            safeClose(connection);
        }
    }

    @Override
    protected void doDetach() throws Exception {
        //System.out.println("Detaching " + getName());
    }

    @Override
    protected void doDelete()
        throws Exception
    {
        Connection connection = provider.dataSource.getConnection();
        PreparedStatement ps = null;
        try
        {
            ps = connection.prepareStatement("DELETE FROM tBlobs WHERE cParent=? AND cName=?");
            setPrimaryKey(ps, this, 0);
            int count = ps.executeUpdate();
            if (count != 0 && count != 1)
                throw new RuntimeException("Corruption suspected, deleting different than 1 (" + count + ") records for " + getName());
            connection.commit();
            connection = null;
            injectType(FileType.IMAGINARY); // TODO: needed?
        }
        finally
        {
            if (connection != null)
                connection.rollback();
            safeClose(ps);
            safeClose(connection);
        }
    }

    @Override
    protected long doGetLastModifiedTime() throws Exception
    {
        return lastModified;
    }

    // doSetLastModifiedTime is not overwritten -> reject

    @Override
    protected Map<String, Object> doGetAttributes() throws Exception {
        // TODO Auto-generated method stub
        return super.doGetAttributes(); // aka empty
    }

    @Override
    protected void doSetAttribute(String attrName, Object value)
                    throws Exception
    {
        // TODO Auto-generated method stub
        super.doSetAttribute(attrName, value);
    }

    @Override
    protected RandomAccessContent doGetRandomAccessContent(RandomAccessMode mode)
                    throws Exception
    {
        // TODO Auto-generated method stub
        return super.doGetRandomAccessContent(mode);
    }

    @Override
    protected OutputStream doGetOutputStream(boolean bAppend) throws Exception
    {
        return new JdbcTableOutputStream(this, bAppend);
    }

    @Override
    protected FileType doGetType() throws Exception
    {
        // TODO
        throw new IllegalStateException("doGetType should not be needed after attach");
    }

    @Override
    protected String[] doListChildren() throws Exception
    {
        return null;
    }

    @Override
    protected long doGetContentSize() throws Exception
    {
        if (contentSize < 0)
        {
            throw new RuntimeException("Cannot determine size, failed to attach " + getName());
        }
        return contentSize;
    }

    @Override
    protected InputStream doGetInputStream() throws Exception
    {
        // TODO: make a stream which can re-open blobs
        byte[] blob = readData();
        return new ByteArrayInputStream(blob);
    }

    @Override
    public FileObject getChild(String file) throws FileSystemException
    {
        FileSystem fs = getFileSystem();
        FileName children = fs.getFileSystemManager().resolveName(getName(), file, NameScope.CHILD);
        return fs.resolveFile(children);
    }

    @Override
    protected void doRename(FileObject newfile)
        throws Exception
    {
        long now = System.currentTimeMillis();
        Connection connection = provider.dataSource.getConnection();
        PreparedStatement ps = null;
        try
        {
            ps = connection.prepareStatement("UPDATE tBlobs SET cParent=?,cName=?,cLastModified=? WHERE cParent=? AND cName=?");
            setPrimaryKey(ps, this, 3);
            setPrimaryKey(ps, (JdbcTableRowFile)newfile, 0);
            ps.setLong(3, now); // TODO: metalast

            int count = ps.executeUpdate();
            if (count != 1)
            {
                throw new RuntimeException("Inconsitent result " + count +" while rename to " + newfile.getName() + " from " + getName());
            }
            connection.commit();
        }
        finally
        {
            safeClose(ps);
            safeClose(connection);
        }
    }

    /**
     * Called by the OutputStream to set the result
     * @throws SQLException
     * @throws FileSystemException
     */
    void writeData(byte[] byteArray) throws SQLException, FileSystemException
    {
        // TODO: needs to handle update/append (MERGE) as well
        long now = System.currentTimeMillis(); // TODO: DB side?
        Connection connection = provider.dataSource.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            ps = connection.prepareStatement("INSERT INTO tBlobs (cParent,cName,cSize,cLastModified,cMarkGarbage,cBlob) VALUES (?,?,?,?,?,?)");
            setPrimaryKey(ps, this, 0);
            ps.setLong(3, byteArray.length);
            ps.setLong(4, now);
            ps.setLong(5, now);
            ps.setBytes(6, byteArray);

            int count = ps.executeUpdate();
            if (count != 1)
            {
                throw new RuntimeException("Inserting different than 1 (" + count + ") records for " + getName());
            }

            connection.commit();
            connection = null;

            lastModified = now;
            contentSize = byteArray.length;

            injectType(FileType.FILE); // TODO: attached?
        }
        finally
        {
            if (connection != null)
            {
                connection.rollback();
            }
            safeClose(rs);
            safeClose(ps);
            safeClose(connection);
        }
    }

    /**
     * Called to read Data for the InputStream
     * @throws SQLException
     */
    byte[] readData() throws IOException, SQLException
    {
        Connection connection = provider.dataSource.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            ps = connection.prepareStatement("SELECT cBlob FROM tBlobs WHERE cParent=? AND cName=?");
            setPrimaryKey(ps, this, 0);
            rs = ps.executeQuery();

            if (rs.next() == false)
            {
                throw new IOException("Database row not found for " + getName()); // TODO: deleted?
            }

            Blob blob = rs.getBlob(1);
            if (blob == null)
            {
                throw new RuntimeException("Blob column is null for " + getName());
            }

            // cannot access Blob after ResultSet#next() or connection#close()
            byte[] bytes = blob.getBytes(1, 1*1014*1024);
            blob.free();

            if (bytes == null)
            {
                throw new RuntimeException("Blob column content is null for " + getName());
            }

            if (rs.next() != false)
            {
                throw new RuntimeException("More than one match for " + getName());
            }

            return bytes;
        }
        finally
        {
            safeClose(rs);
            safeClose(ps);
            safeClose(connection);
        }
    }

    /**
     * Sets primary key of this file on prepared statement.
     * <P>
     * Supports multi column keys (driven by {@link #getKeys(JdbcTableRowFile)}).
     *
     * @param before number of bind parameters before first key, typically 0
     * @throws SQLException
     * @throws FileSystemException
     * */
    private void setPrimaryKey(PreparedStatement ps, JdbcTableRowFile file, int before) throws SQLException, FileSystemException
    {
        String[] keys = getKeys(file);
        for(int i=0;i<keys.length;i++)
        {
            ps.setString(before + i + 1, keys[i]);
        }
    }

    /**
     * Convert file name into composed key.
     *
     * @param newfile
     * @return
     * @throws FileSystemException
     */
    private String[] getKeys(FileObject file) throws FileSystemException
    {
        return new String[] { file.getName().getParent().getPathDecoded(), file.getName().getBaseName() };
    }


    private void safeFree(Blob blob)
    {
        try { blob.free(); } catch (Exception ignored) { }
    }

    private void safeClose(Connection connection)
    {
        try { connection.close(); } catch (Exception ignored) { }
    }

    private void safeClose(PreparedStatement ps)
    {
        try { ps.close(); } catch (Exception ignored) { }
    }

    private void safeClose(ResultSet rs)
    {
        try { rs.close(); } catch (Exception ignored) { }

    }
}
