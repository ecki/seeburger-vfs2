package com.seeburger.vfs2.provider.jdbctable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.vfs2.FileContentInfoFactory;
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
            //cID varchar(30) PRIMARY KEY,
            //cSize BIGINT,
            //cLastModifies BIGINT,
            //cGeneration BIGINT,
            //cBlob BLOB);
            ps = connection.prepareStatement("SELECT cSize,cLastModified,cBlob FROM tBlobs WHERE cID=?");
            setPrimaryKey(ps, 0);
            rs = ps.executeQuery();
            if (!rs.next())
            {
                injectType(FileType.IMAGINARY);
                return;
            }
            contentSize  = rs.getLong(1);
            lastModified = rs.getLong(2);
            blob = rs.getBlob(3);

            if (blob == null)
                throw new RuntimeException("Critical inconsitency, blob column is null for " +getName());

            long blobLength = (blob!=null)?blob.length():-1;

            if (blobLength != contentSize) // TOOD compressed
                throw new RuntimeException("Critical inconsitency between metadata (" + contentSize + ") and BLOB (" + blobLength + " for " + getName());

            if (rs.next())
            {
                throw new RuntimeException("Critical consitency problem, duplicate response to " + getName());
            }

            injectType(FileType.FILE);
        }
        finally
        {
            safeFree(blob);
            safeClose(rs);
            safeClose(ps);
            safeClose(connection);
        }
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

    @Override
    protected void doDetach() throws Exception {
        System.out.println("Detaching " + getName());
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
                    throws Exception {
        // TODO Auto-generated method stub
        super.doSetAttribute(attrName, value);
    }

    @Override
    protected RandomAccessContent doGetRandomAccessContent(RandomAccessMode mode)
                    throws Exception {
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
            throw new RuntimeException("Cannot determine size, failed to attach " + getName());
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

    /**
     * Called by the OutputStream to set the result
     * @throws SQLException
     */
    void writeData(byte[] byteArray) throws SQLException
    {
        long now = System.currentTimeMillis(); // TODO: DB side?
        Connection connection = provider.dataSource.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            //	    	cID varchar(30) PRIMARY KEY,
            //	    	cSize BIGINT NOT NULL,
            //	    	cLastModified BIGINT NOT NULL,
            //	    	cMarkGarbage BIGINT,
            //	    	cUserMark1 BIGINT,
            //	    	cUserMark2 BIGINT,
            //	    	cBlob BLOB);
            ps = connection.prepareStatement("INSERT INTO tBlobs (cID,cSize,cLastModified,cMarkGarbage,cBlob) VALUES (?,?,?,?,?)");
            setPrimaryKey(ps, 0);
            ps.setLong(2, byteArray.length);
            ps.setLong(3, now);
            ps.setLong(4, now);
            ps.setBytes(5, byteArray);
            int count = ps.executeUpdate();
            if (count != 1)
                throw new RuntimeException("Inserting different than 1 (" + count + ") records for " + getName());
            connection.commit();
            connection = null;
            lastModified = now;
            contentSize = byteArray.length;
            // TODO: attached?
            injectType(FileType.FILE);
        }
        finally
        {
            if (connection != null)
                connection.rollback();
            safeClose(rs);
            safeClose(ps);
            safeClose(connection);
        }
    }

    /**
     * Called to read Data for the InputStream
     * @throws SQLException
     */
    byte[] readData() throws SQLException
    {
        Connection connection = provider.dataSource.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            ps = connection.prepareStatement("SELECT cBlob FROM tBlobs WHERE cID=?");
            setPrimaryKey(ps, 0);
            rs = ps.executeQuery();

            if (rs.next() == false)
                throw new RuntimeException("Database row not found for "+getName()); // TODO: deleted?

            Blob blob = rs.getBlob(1);
            if (blob == null)
                throw new RuntimeException("Blob column is null for "+getName());

            // cannot access Blob after ResultSet#next() or connection#close()
            byte[] bytes = blob.getBytes(1, 1*1014*1024);
            if (bytes == null)
                throw new RuntimeException("Blob column content is null for " + getName());

            if (rs.next() != false)
                throw new RuntimeException("More than one match for " + getName());

            return bytes;
        }
        finally
        {
            //safeFree(blob);
            safeClose(rs);
            safeClose(ps);
            safeClose(connection);
        }
    }

    /**
     * Sets primary key of this file on resultset.
     *
     * @param before number of bind parameters before first key, typically 0
     * @throws SQLException
     * */
    private void setPrimaryKey(PreparedStatement ps, int before) throws SQLException
    {
        String name = getName().getBaseName();
        ps.setString(before + 1, name);
    }
}
