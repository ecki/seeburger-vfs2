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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileNotFolderException;
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
    public static class DataDescription
    {
        long generation;
        long dataLength;

        byte[] buffer;
        long pos;
    }

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
            ps = connection.prepareStatement("SELECT cSize,cLastModified,cBlob FROM tBlobs WHERE (cParent=? AND cName=?)");
            setPrimaryKey(ps, this, 0);
            rs = ps.executeQuery();
            if (!rs.next())
            {
                injectType(FileType.IMAGINARY);
                return;
            }
            long size = rs.getLong(1);
            lastModified = rs.getLong(2);
            if (size == -2)
            {
                injectType(FileType.FOLDER);
            }
            else
            {
                blob = rs.getBlob(3);
                if (blob == null)
                {
                    throw new IOException("Critical inconsitency, blob column is null for " +getName());
                }

                contentSize = blob.length();
                injectType(FileType.FILE);
            }


            // TODO: additional attributes

            if (rs.next())
            {
                throw new IOException("Critical consitency problem, duplicate response to " + getName());
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
            ps = connection.prepareStatement("INSERT INTO tBlobs (cParent,cName,cSize,cLastModified,cMarkGarbage) VALUES (?,?,-2,?,?)");
            setPrimaryKey(ps, this, 0);
            ps.setLong(3, now);
            ps.setLong(4, now);

            int count = ps.executeUpdate();
            if (count != 1)
            {
                throw new IOException("Inserting different than 1 (" + count + ") records for " + getName());
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
            ps = connection.prepareStatement("DELETE FROM tBlobs WHERE cParent=? AND cName=?"); // TODO: recursive?
            setPrimaryKey(ps, this, 0);
            int count = ps.executeUpdate();
            if (count != 0 && count != 1)
                throw new IOException("Corruption suspected, deleting different than 1 (" + count + ") records for " + getName());
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

    // TODO: it is better to instead implement #doListChildrenResolved() to save the attachs -> attachFile(rs) method
    @Override
    protected String[] doListChildren() throws Exception
    {
        if (!getType().hasChildren())
        {
            throw new FileNotFolderException(this);
        }
        Connection connection = provider.dataSource.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            List<String> children = new ArrayList<String>(32);
            ps = connection.prepareStatement("SELECT cName FROM tBlobs WHERE cParent=?"); // TODO: order?
            ps.setString(1, getName().getPathDecoded());
            rs = ps.executeQuery();
            while(rs.next())
            {
                String name = rs.getString(1);
                children.add(name);
            }
            return children.toArray(new String[children.size()]);
        }
        finally
        {
            safeClose(rs);
            safeClose(ps);
            safeClose(connection);
        }
    }

    @Override
    protected long doGetContentSize() throws Exception
    {
        if (contentSize < 0)
        {
            throw new IOException("Cannot determine size, failed to attach " + getName());
        }
        return contentSize;
    }

    @Override
    protected InputStream doGetInputStream() throws Exception
    {
        if (!getType().hasContent())
            throw new FileSystemException("vfs.provider/read-not-file.error", getName());

        // the input stream will open this row/blob multiple times to fetch new buffers
        return new JdbcTableInputStream(this);
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
                throw new IOException("Inconsitent result " + count +" while rename to " + newfile.getName() + " from " + getName());
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
    void writeData(byte[] byteArray, boolean append) throws Exception
    {
        long now = System.currentTimeMillis(); // TODO: DB side?
        // TODO: needs to handle concurrent modifications (i.e. changes since attach)
        if (exists())
        {
            if (append)
                writeDataUpdate(now, byteArray);
            else
                writeDataOverwrite(now, byteArray);
        } else {
            writeDataInsert(now, byteArray);
        }
    }

    private void writeDataOverwrite(long now, byte[] byteArray) throws SQLException, IOException
    {
        Connection connection = provider.dataSource.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            ps = connection.prepareStatement("UPDATE tBlobs SET cSize=?, cLastModified=?, cMarkGarbage=?, cBlob=? WHERE (cParent=? AND cName=?)");
            setPrimaryKey(ps, this, 4);
            ps.setLong(1, byteArray.length);
            ps.setLong(2, now);
            ps.setLong(3, now);
            ps.setBytes(4, byteArray);

            int count = ps.executeUpdate();
            if (count != 1)
            {
                throw new IOException("Updating different than 1 (" + count + ") records for " + getName());
            }

            connection.commit(); connection.close(); connection = null; // TODO: null/close?

            lastModified = now;
            contentSize = byteArray.length;

            try
            {
                endOutput(); // setsFile type (and trigger notifications)
            }
            catch (FileSystemException fse)
            {
                throw fse;
            }
            catch (Exception e)
            {
                throw new IOException(e);
            }
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

    private void writeDataUpdate(long now, byte[] byteArray) throws SQLException, IOException
    {
        Connection connection = provider.dataSource.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        Blob blob = null;
        try
        {
            ps = connection.prepareStatement("SELECT cBlob, cSize, cMarkGarbage, cLastModified FROM tBlobs WHERE (cParent=? AND cName=?) FOR UPDATE", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            setPrimaryKey(ps, this, 0);
            rs = ps.executeQuery();

            if (rs.next() == false)
            {
                throw new IOException("Database row not found for " + getName()); // TODO: deleted?
            }

            blob = rs.getBlob(1);
            if (blob == null)
            {
                throw new IOException("Blob column is null for " + getName());
            }
            final long newLength = blob.length() + byteArray.length;
            blob.setBytes(blob.length() +1 , byteArray);
            rs.updateBlob(1, blob);
            rs.updateLong(2, newLength);
            rs.updateLong(3, now);
            rs.updateLong(4, now);

            rs.updateRow();

            if (rs.next() != false)
            {
                throw new IOException("More than one match for " + getName());
            }

            connection.commit(); connection.close(); connection = null; // TODO: null/close?

            lastModified = now;
            contentSize = newLength;

            try
            {
                endOutput(); // setsFile type (and trigger notifications)
            }
            catch (FileSystemException fse)
            {
                throw fse;
            }
            catch (Exception e)
            {
                throw new IOException(e);
            }
        }
        finally
        {
            if (connection != null)
            {
                connection.rollback();
            }
            safeFree(blob);
            safeClose(rs);
            safeClose(ps);
            safeClose(connection);
        }
    }

    private void writeDataInsert(long now, byte[] byteArray) throws SQLException, IOException
    {
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
                throw new IOException("Inserting different than 1 (" + count + ") records for " + getName());
            }

            connection.commit(); connection.close(); connection = null; // TODO: null/close?

            lastModified = now;
            contentSize = byteArray.length;

            try
            {
                endOutput(); // setsFile type (and trigger notifications)
            }
            catch (FileSystemException fse)
            {
                throw fse;
            }
            catch (Exception e)
            {
                throw new IOException(e);
            }
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
     * @param len the maximum length of buffer to return
     * @param position in stream, first byte is pos=0
     * @throws SQLException
     */
    byte[] readData(long pos, int len) throws IOException
    {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            connection = provider.dataSource.getConnection();
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
                throw new IOException("Blob column is null for " + getName());
            }

            // cannot access Blob after ResultSet#next() or connection#close()
            byte[] bytes = blob.getBytes(pos+1, len);
            blob.free();

            if (bytes == null)
            {
                throw new IOException("Blob column content is null for " + getName());
            }

            if (rs.next() != false)
            {
                throw new IOException("More than one match for " + getName());
            }

            return bytes;
        }
        catch (SQLException ex)
        {
            throw new IOException("database problem while reading blob.", ex);
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
        return new String[] {
                              file.getName().getParent().getPathDecoded(),
                              file.getName().getBaseName() };
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

    DataDescription startReadData(int bufsize) throws IOException
    {
        // TODO: synchronized
        long currentSize = getContent().getSize();
        long currentGen = 1l; // TODO - lastModified?

        DataDescription desc = new DataDescription();
        desc.generation = currentGen;
        desc.dataLength = currentSize;
        desc.pos = 0;
        desc.buffer = readData(0, bufsize); // might be smaller

        return desc;
    }

    void nextReadData(DataDescription desc, int bufsize) throws IOException
    {
        long currentSize = getContent().getSize();
        long currentGen = 1l;

        if (desc.dataLength != currentSize || desc.generation != currentGen)
        {
            throw new IOException("Input Stream cannot be read on position " + desc.pos + " - content has changed.");
        }

        int len = (int)Math.min(bufsize,  desc.dataLength - desc.pos);
        desc.buffer = readData(desc.pos, len);

        return;
    }

}
