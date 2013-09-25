package com.seeburger.vfs2.provider.jdbctable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
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
    /** value of size entry for folders in db. */
    final static long FOLDER_SIZE = -2;
    /** size of virtual files (never in DB) */
    final static long VIRTUAL_SIZE = -1;

    public static class DataDescription
    {
        long generation;
        long dataLength;

        byte[] buffer;
        long pos;
    }

    long lastModified = -1;
    long contentSize = VIRTUAL_SIZE;
    JdbcDialect dialect;

    public JdbcTableRowFile(AbstractFileName name, JdbcTableFileSystem fs)
    {
        super(name, fs);
        dialect = fs.getDialect();
    }

    @Override
    protected void doAttach() throws Exception
    {
        Connection con = getConnection("doAttach");
        PreparedStatement ps = null;
        ResultSet rs = null;
        Blob blob = null;
        try
        {
            ps = dialect.prepareQuery(con, "SELECT cSize,cLastModified,cBlob FROM {table} WHERE (cParent=? AND cName=?)");
            setPrimaryKey(ps, this, 0);
            rs = ps.executeQuery();
            if (!rs.next())
            {
                injectType(FileType.IMAGINARY);
                return;
            }
            long size = rs.getLong(1);
            lastModified = rs.getLong(2);
            if (size == FOLDER_SIZE)
            {
                injectType(FileType.FOLDER);
            }
            else
            {
                blob = rs.getBlob(3);
                if (blob == null && size > 0)
                {
                    throw new IOException("Critical inconsitency, blob column is null for " +getName());
                }

                contentSize = size;
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
            closeConnection(blob, rs, ps, con);
        }
    }

    @Override
    protected void doCreateFolder()
        throws Exception
    {
        long now = System.currentTimeMillis(); // TODO: DB side?
        Connection con = getConnection("doCreateFolder");
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            ps = dialect.prepareQuery(con, "INSERT INTO {table} (cParent,cName,cSize,cLastModified,cMarkGarbage) VALUES (?,?,?,?,?)");
            setPrimaryKey(ps, this, 0);
            ps.setLong(3, FOLDER_SIZE);
            ps.setLong(4, now);
            ps.setLong(5, now);

            int count = ps.executeUpdate();
            if (count != 1)
            {
                throw new IOException("Inserting different than 1 (" + count + ") records for " + getName());
            }

            // TODO: update last modified of parent

            con.commit();
            con.close(); con = null;

            lastModified = now;
            contentSize = -1;
        }
        finally
        {
            rollbackConnection(null, rs, ps, con);
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
        Connection con = getConnection("doDelete");
        PreparedStatement ps = null;
        try
        {
            ps = dialect.prepareQuery(con, "DELETE FROM {table} WHERE cParent=? AND cName=?"); // TODO: ensure no children?
            setPrimaryKey(ps, this, 0);
            int count = ps.executeUpdate();

            if (count == 0 || count == 1)
            {
                if (count == 0)
                {
                    // Derby generates warnings on no-match: https://issues.apache.org/jira/browse/DERBY-448
                    ps.clearWarnings();
                }
                con.commit();
                con.close(); con = null;
                return;
            }

            // count > 1
            throw new IOException("Corruption suspected, deleting different than 1 (" + count + ") records for " + getName());
        }
        finally
        {
            rollbackConnection(null, null, ps, con);
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
        Connection con = getConnection("doListChildren");
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            List<String> children = new ArrayList<String>(32);
            ps = dialect.prepareQuery(con, "SELECT cName FROM {table} WHERE cParent=?");
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
            closeConnection(null, rs, ps, con);
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
        Connection con = getConnection("doRename");
        long now = System.currentTimeMillis();
        PreparedStatement ps = null;
        try
        {
            ps = dialect.prepareQuery(con, "UPDATE {table} SET cParent=?,cName=?,cLastModified=? WHERE cParent=? AND cName=?");
            setPrimaryKey(ps, this, 3);
            setPrimaryKey(ps, (JdbcTableRowFile)newfile, 0);
            ps.setLong(3, now);

            int count = ps.executeUpdate();
            if (count == 0)
            {
                // Derby generates warnings on no-match: https://issues.apache.org/jira/browse/DERBY-448
                ps.clearWarnings();
                throw new IOException("No file to rename to " + newfile.getName() + " from " + getName());
            }

            if (count != 1)
            {
                throw new IOException("Inconsitent result " + count +" while rename to " + newfile.getName() + " from " + getName());
            }

            // TODO: update parent lastModified

            con.commit();
            con.close(); con = null;
        }
        finally
        {
            rollbackConnection(null, null, ps, con);
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
        Connection con = getConnection("writeDataOverwrite");
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            ps = dialect.prepareQuery(con, "UPDATE {table} SET cSize=?, cLastModified=?, cMarkGarbage=?, cBlob=? WHERE (cParent=? AND cName=?)");
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

            con.commit(); // TODO: move behind endOutput?
            con.close(); con = null;

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
            rollbackConnection(null, rs, ps, con);
        }
    }

    private void writeDataUpdate(long now, byte[] byteArray) throws SQLException, IOException
    {
        Connection con = getConnection("writeDataUpdate");
        PreparedStatement ps = null;
        ResultSet rs = null;
        Blob blob = null;
        try
        {
            // some DB (like H2) require the PK Columns in the Result Set to be able to use updateRow()
            //ps = dialect.prepareUpdateable(con, "SELECT cBlob, cSize, cMarkGarbage, cLastModified, cParent, cName FROM {table} WHERE (cParent=? AND cName=?) FOR UPDATE");
            ps = dialect.prepareUpdateable(con, "SELECT cBlob, cSize, cMarkGarbage, cLastModified, cParent, cName FROM {table} WHERE (cParent=? AND cName=?) {FOR UPDATE}");
            setPrimaryKey(ps, this, 0);
            rs = ps.executeQuery();

            if (rs.next() == false)
            {
                throw new IOException("Database row not found for " + getName()); // TODO: deleted -> insert?
            }

            blob = rs.getBlob(1);
            if (blob == null)
            {
                throw new IOException("Blob column is null for " + getName());
            }

            final long newLength = blob.length() + byteArray.length;
            if (dialect.supportsAppendBlob())
            {
                blob.setBytes(blob.length() + 1 , byteArray);
            }
            else
            {
                final int oldLen = (int)blob.length();
                byte[] buf = new byte[(int)newLength];
                byte[] oldBuf = blob.getBytes(1, oldLen);
                System.arraycopy(oldBuf, 0, buf, 0, oldLen);
                System.arraycopy(byteArray, 0, buf, oldLen, byteArray.length);
                blob.setBytes(1, buf);
            }
            rs.updateBlob(1, blob);
            rs.updateLong(2, newLength);
            rs.updateLong(3, now);
            rs.updateLong(4, now);

            rs.updateRow();

            if (rs.next() != false)
            {
                throw new IOException("More than one match for " + getName());
            }

            con.commit();
            con.close(); con = null;

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
            rollbackConnection(blob, rs, ps, con);
        }
    }

    private void writeDataInsert(long now, byte[] byteArray) throws SQLException, IOException
    {
        Connection con = getConnection("writeDataInsert");
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            ps = dialect.prepareQuery(con, "INSERT INTO {table} (cParent,cName,cSize,cLastModified,cMarkGarbage,cBlob) VALUES (?,?,?,?,?,?)");
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

            con.commit();
            con.close(); con = null;

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

            // TODO: update last modified of parent

        }
        finally
        {
            rollbackConnection(null, rs, ps, con);
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
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Blob blob = null;
        try
        {
            con = getConnection("readData"); // inside try because rethrown as IOException
            ps = dialect.prepareQuery(con, "SELECT cSize,cBlob FROM {table} WHERE cParent=? AND cName=?");
            setPrimaryKey(ps, this, 0);
            rs = ps.executeQuery();

            if (rs.next() == false)
            {
                throw new IOException("Database row not found for " + getName()); // TODO: Filenotfound exception?
            }

            final long size = rs.getLong(1);
            blob = rs.getBlob(2);
            if (size != 0 && blob == null)
            {
                throw new IOException("Blob column is null, expecting " + size + " bytes for " + getName());
            }

            // cannot access Blob after ResultSet#next() or connection#close()

            byte[] bytes;

            if (pos > size)
            {
                throw new IOException("Requesting position " + pos + " but Blob size is " + size + " for file " + getName());
            }

            // Oracle might have null for empty blob, so we don't touch it at all
            if (size == 0)
                bytes = new byte[0];
            else
                bytes = blob.getBytes(pos+1, len);

            if (bytes == null)
            {
                throw new IOException("Blob column content is null, expecting " + size + " bytes for " + getName());
            }

            if (rs.next() != false)
            {
                throw new IOException("Consitency Problem, more than one Database row for " + getName());
            }

            return bytes;
        }
        catch (SQLException ex)
        {
            // TODO: retry?
            throw new IOException("Database problem while reading blob for " + getName(), ex);
        }
        finally
        {
            closeConnection(blob, rs, ps, con);
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

    private Connection getConnection(String where) throws SQLException
    {
        return dialect.getConnection();
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
        catch (NullPointerException ignored) { } // Oracle Driver Problem
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
