/*
 * JdbcTableRowFile.java
 *
 * created at 2013-08-09 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileNotFolderException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;


public class JdbcTableRowFile extends AbstractFileObject<JdbcTableFileSystem>
{
    /** value of size entry for folders in db. */
    static final long FOLDER_SIZE = -2;
    /** size of virtual files (never in DB) */
    static final long VIRTUAL_SIZE = -1;

    /** used when empty array is needed */
    static private final byte[] EMPTY_BYTES = new byte[0];

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
        for(int retry=0; ; retry++)
        {
            Connection con = getConnection("doAttach");
            PreparedStatement ps = null;
            ResultSet rs = null;
            try
            {
                ps = dialect.prepareQuery(con, "SELECT cSize,cLastModified FROM {table} WHERE (cParent=? AND cName=?)");
                setPrimaryKey(ps, this, 0);
                rs = ps.executeQuery();
                if (!rs.next())
                {
                    contentSize = VIRTUAL_SIZE;
                    injectType(FileType.IMAGINARY);
                    return;
                }
                long size = rs.getLong(1);
                lastModified = rs.getLong(2);
                if (size == FOLDER_SIZE)
                {
                    contentSize = FOLDER_SIZE;
                    injectType(FileType.FOLDER);
                }
                else
                {
                    contentSize = size;
                    injectType(FileType.FILE);
                }

                if (rs.next())
                {
                    // should not happen since there is a PK on cParent/cName
                    throw new IOException("Critical consistency problem. Duplicate records in table=" + dialect.getQuotedTable() + " for name=" + getName());
                }

                return; // exit retry loop
            }
            catch (SQLException e)
            {
                if (!dialect.shouldRetry(e, retry))
                {
                    throw e;
                }
            }
            finally
            {
                closeConnection(null, rs, ps, con);
            }
        } // retry
    }

    @Override
    protected void doCreateFolder() throws Exception
    {
        for(int retry=0; ; retry++)
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
                    if (count == 0)
                    {
                        ps.clearWarnings(); // Derby generates warnings on no-match: https://issues.apache.org/jira/browse/DERBY-448
                    }
                    throw new IOException("Critical consistency problem, result count=" + count + "while inserting new directory=" + getName());
                }

                // TODO: update last modified of parent

                con.commit();
                dialect.returnConnection(con); con = null;

                lastModified = now;
                contentSize = FOLDER_SIZE;

                return;
            } // TODO: add a more helpful diagnostic?
            catch (SQLException e)
            {
                if (!dialect.shouldRetry(e, retry))
                {
                    throw e;
                }
            }
            finally
            {
                rollbackConnection(null, rs, ps, con);
            }
        } // retry
    }

    //@Override
    //protected void doDetach() throws Exception {
        // nothing needs to be done
    //}

    @Override
    protected void doDelete() throws Exception
    {
        for(int retry=0; ; retry++)
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
                        ps.clearWarnings(); // Derby generates warnings on no-match: https://issues.apache.org/jira/browse/DERBY-448
                    }
                    con.commit();
                    dialect.returnConnection(con); con = null;

                    contentSize = VIRTUAL_SIZE;

                    return;
                }

                // count > 1
                throw new IOException("Critical consistency problem, result count=" + count + " for deleting name=" + getName());
            }
            catch (SQLException e)
            {
                if (!dialect.shouldRetry(e, retry))
                {
                    throw e;
                }
            }
            finally
            {
                rollbackConnection(null, null, ps, con);
            }
        } // retry
    }

    @Override
    protected long doGetLastModifiedTime() throws Exception
    {
        // from doAttach
        return lastModified;
    }

    // doSetLastModifiedTime is not overwritten -> throws last modified not supported
    // doGetRandomAccessContent(RandomAccessMode -> throws random access not supported

    @Override
    protected Map<String, Object> doGetAttributes() throws Exception
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        long markTime = getMarkTime();
        attributes.put("markTime", Long.valueOf(markTime));
        return attributes;
    }

    @Override
    protected void doSetAttribute(String attrName, Object value)
                    throws Exception
    {
        if ("markTime".equalsIgnoreCase(attrName))
        {
            long time = 0;
            if (value == null)
            {
                time = System.currentTimeMillis();
            }
            else if (value instanceof Long)
            {
                time = ((Long)value).longValue();
            }
            else if (value instanceof String)
            {
                time = Long.parseLong((String)value);
            }
            else
            {
                throw new IllegalArgumentException("attribute markTime requires null, Long or String as type. was=" + value.getClass());
            }
            setMarkTime(time);
        }
        else
        {
            super.doSetAttribute(attrName, value); // throws attribute not supported
        }
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

        for(int retry=0; ; retry++)
        {
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
            catch (SQLException e)
            {
                if (!dialect.shouldRetry(e, retry))
                {
                    throw e;
                }
            }
            finally
            {
                closeConnection(null, rs, ps, con);
            }
        } // retry
    }

    @Override
    protected long doGetContentSize() throws Exception
    {
        if (contentSize < 0)
        {
            throw new IOException("Cannot determine size, failed to attach name=" + getName());
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
        // this avoids getChildren() as used by super.getChild
        FileSystem fs = getFileSystem();
        FileName children = fs.getFileSystemManager().resolveName(getName(), file, NameScope.CHILD);
        return fs.resolveFile(children);
    }

    @Override
    protected void doRename(FileObject newfile) throws Exception
    {
        for(int retry=0; ; retry++)
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
                if (count != 1)
                {
                    if (count == 0)
                    {
                        ps.clearWarnings(); // Derby generates warnings on no-match: https://issues.apache.org/jira/browse/DERBY-448
                    }
                    throw new IOException("Critical consistency problem, result count=" + count +" while rename to name=" + newfile.getName() + " from name=" + getName());
                }

                // TODO: update parent lastModified

                con.commit();
                dialect.returnConnection(con); con = null;

                return;
            }
            catch (SQLException e)
            {
                if (!dialect.shouldRetry(e, retry))
                {
                    throw e;
                }
            }
            finally
            {
                rollbackConnection(null, null, ps, con);
            }
        } // retry
    }

    /**
     * Called by the OutputStream to set the result.
     * <p>
     * This implementation will use {@link #writeDataInsert(long, byte[])}
     * or {@link #writeDataUpdate(long, byte[])}
     * or {@link #writeDataOverwrite(long, byte[])} depending if data exists
     * and the {@code append} mode.
     *
     *
     * @throws IOException if underlying operation throws
     * @throws SQLException if underlying operation throws
     * @throws FileSystemException if underlying operation throws
     */
    void writeData(byte[] byteArray, boolean append) throws FileSystemException, SQLException, IOException
    {
        long timeStamp = System.currentTimeMillis(); // TODO: DB side?
        // TODO: needs to handle concurrent modifications (i.e. changes since attach)
        if (exists())
        {
            if (append)
                writeDataUpdate(timeStamp, byteArray);
            else
                writeDataOverwrite(timeStamp, byteArray);
        } else {
            writeDataInsert(timeStamp, byteArray);
        }
    }

    private void writeDataOverwrite(long now, byte[] byteArray) throws SQLException, IOException
    {
        for(int retry=0; ; retry++)
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
                ps.setBytes(4, byteArray); // no ned for supportsBlob()

                int count = ps.executeUpdate();
                if (count != 1)
                {
                    if (count == 0)
                    {
                        ps.clearWarnings();  // Derby generates warnings on no-match: https://issues.apache.org/jira/browse/DERBY-448
                    }
                    throw new IOException("Critical consistency problem, result count=" + count + " while updating name=" + getName());
                }

                con.commit(); // TODO: move behind endOutput?
                dialect.returnConnection(con); con = null;

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

                return;
            }
            catch (SQLException e)
            {
                if (!dialect.shouldRetry(e, retry))
                {
                    throw e;
                }
            }
            finally
            {
                rollbackConnection(null, rs, ps, con);
            }
        } // retry
    }

    private void writeDataUpdate(long now, byte[] byteArray) throws SQLException, IOException
    {
        for(int retry=0; ; retry++)
        {
            Connection con = getConnection("writeDataUpdate");
            PreparedStatement ps = null;
            ResultSet rs = null;
            Blob blob = null;
            try
            {
                // some DB (like H2) require the PK Columns in the Result Set to be able to use updateRow()
                ps = dialect.prepareUpdateable(con, "SELECT cBlob, cSize, cMarkGarbage, cLastModified, cParent, cName FROM {table} WHERE (cParent=? AND cName=?) {FOR UPDATE}");
                setPrimaryKey(ps, this, 0);
                rs = ps.executeQuery();

                if (!rs.next())
                {
                    throw new IOException("Database row not found for name=" + getName()); // TODO: deleted -> insert?
                }

                final long newLength;
                if (dialect.supportsBlob())
                {
                    blob = rs.getBlob(1);
                    if (blob == null) // TODO size > 0?
                    {
                        throw new IOException("Critical consistency problem, Blob column is null for name=" + getName());
                    }

                    newLength = blob.length() + byteArray.length;
                    if (dialect.supportsAppendBlob())
                    {
                        blob.setBytes(blob.length() + 1 , byteArray);
                    }
                    else
                    {
                        final int oldLen = (int)blob.length();
                        byte[] oldBuf = blob.getBytes(1, oldLen);
                        final byte[] buf = Arrays.copyOf(oldBuf, (int)newLength);
                        oldBuf=null;
                        System.arraycopy(byteArray, 0, buf, oldLen, byteArray.length);
                        blob.setBytes(1, buf);
                    }
                    rs.updateBlob(1, blob);
                } else {
                    byte[] oldBuf = rs.getBytes(1);
                    if (oldBuf == null) // TODO size > 0?
                    {
                        throw new IOException("Critical consistency problem, Blob column is null for name=" + getName());
                    }
                    newLength = oldBuf.length + byteArray.length;
                    final int oldLen = oldBuf.length;
                    final byte[] buf = Arrays.copyOf(oldBuf, (int)newLength);
                    oldBuf = null;
                    System.arraycopy(byteArray, 0, buf, oldLen, byteArray.length);
                    rs.updateBytes(1, buf);
                }
                rs.updateLong(2, newLength);
                rs.updateLong(3, now);
                rs.updateLong(4, now);

                rs.updateRow();

                if (rs.next())
                {
                    throw new IOException("More than one match for name=" + getName());
                }

                con.commit();
                dialect.returnConnection(con); con = null;

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

                return;
            }
            catch (SQLException e)
            {
                if (!dialect.shouldRetry(e, retry))
                {
                    throw e;
                }
            }
            finally
            {
                rollbackConnection(blob, rs, ps, con);
            }
        } // retry
    }

    private void writeDataInsert(long now, byte[] byteArray) throws SQLException, IOException
    {
        for(int retry=0; ; retry++)
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
                ps.setBytes(6, byteArray); // no need for supportBlob()

                int count = ps.executeUpdate();
                if (count != 1)
                {
                    if (count == 0)
                    {
                        ps.clearWarnings(); // Derby generates warnings on no-match: https://issues.apache.org/jira/browse/DERBY-448
                    }
                    throw new IOException("Critical consistency problem, result count=" + count + " while inserting name=" + getName());
                }

                con.commit();
                dialect.returnConnection(con); con = null;

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

                return;
            }
            catch (SQLException e)
            {
                if (!dialect.shouldRetry(e, retry))
               {
                    throw e;
                }
            }
            finally
            {
                rollbackConnection(null, rs, ps, con);
            }
        } // retry
    }

    /**
     * Called to read Data for the InputStream
     * @param pos in stream, first byte is pos=0
     * @param len the maximum length of buffer to return
     * @throws SQLException
     */
    byte[] readData(long pos, int len) throws IOException
    {
        for(int retry=0; ; retry++)
        {
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            Blob blob = null;
            long size = -1;
            try
            {
                con = getConnection("readData"); // inside try because rethrown as IOException
                ps = dialect.prepareQuery(con, "SELECT cSize,cBlob FROM {table} WHERE cParent=? AND cName=?");
                setPrimaryKey(ps, this, 0);
                rs = ps.executeQuery();

                if (!rs.next())
                {
                    throw new IOException("Database row not found for name=" + getName()); // TODO: Filenotfound exception?
                }

                size = rs.getLong(1);

                if (pos > size)
                {
                    throw new IOException("Requested position=" + pos + " exceeds size=" + size + " for name=" + getName());
                }
                // TODO: pos+len > size

                final byte[] bytes;
                if (dialect.supportsBlob())
                {

                    // cannot access Blob after ResultSet#next() or connection#close()

                    if (size == 0)
                    {
                        // Oracle might have null for empty blob, so we don't touch it at all
                        bytes = EMPTY_BYTES;
                    }
                    else
                    {
                        blob = rs.getBlob(2);
                        if (blob == null)
                        {
                            throw new IOException("Critical consistency problem, Blob column is null while expecting size=" + size + " bytes for name=" + getName());
                        }
                        bytes = blob.getBytes(pos+1, len);
                    }

                    if (bytes == null)
                    {
                        throw new IOException("Critical consistency problem, Blob column content is null when expecting size=" + size + " bytes for name=" + getName());
                    }
                } else {
                    if (size == 0)
                    {
                        bytes = EMPTY_BYTES;
                    }
                    else
                    {
                        byte[] buf = rs.getBytes(2);
                        if (buf == null)
                        {
                            throw new IOException("Critical consistency problem, Blob column is null while expecting size=" + size + " bytes for name=" + getName());
                        }
                        bytes = Arrays.copyOfRange(buf, (int)pos, len);
                        buf = null;
                    }
                }

                if (rs.next())
                {
                    throw new IOException("Critical consistency problem, more than one Database row for name=" + getName());
                }

                return bytes;
            }
            catch (SQLException e)
            {
                if (!dialect.shouldRetry(e, retry))
                {
                    throw new IOException("Database problem while reading blob for name=" + getName() + ". pos=" + pos + ", len=" + len + ", size=" + size, e);
                }
            }
            finally
            {
                closeConnection(blob, rs, ps, con);
            }
        } // retry
    }


    /**
     * Sets primary key of this file on prepared statement.
     * <P>
     * Supports multi-column keys (driven by {@link #getKeys(JdbcTableRowFile)}).
     *
     * @param ps the prepared statement on which the parameters will be set
     * @param file the file to extrace key from (uses {@link #getKeys(FileObject)})
     * @param before number of bind parameters before first key, typically 0
     * @throws SQLException if seString on prepared statement throws
     * @throws FileSystemException if {@link #getKeys(FileObject)} throws
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
     * @param file the instance of FileObject
     * @return tuple with file parent path and name to be used as keys
     * @throws FileSystemException
     */
    private String[] getKeys(FileObject file) throws FileSystemException
    {
        FileName parent = file.getName().getParent();
        String parentName;
        String baseName;

        // Oracle does not like Null name/parent, so we insert // for root-dir
        if (parent == null)
        {
            parentName="//";
            baseName = "//";
        }
        else
        {
            parentName = parent.getPathDecoded();
            baseName = file.getName().getBaseName();
        }

        return new String[] { parentName, baseName };
    }


    DataDescription startReadData(int bufsize) throws IOException
    {
        // TODO: synchronized
        long currentSize = getContent().getSize();
        long currentGen = 1L; // TODO - lastModified?

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
        long currentGen = 1L;

        if (desc.dataLength != currentSize || desc.generation != currentGen)
        {
            throw new IOException("Input Stream cannot be read on position " + desc.pos + " - content has changed.");
        }

        int len = (int)Math.min(bufsize,  desc.dataLength - desc.pos);
        desc.buffer = readData(desc.pos, len);

        return;
    }

    private void setMarkTime(long time) throws SQLException, FileSystemException
    {
        for(int retry=0; ; retry++)
        {
            Connection con = getConnection("mark");
            PreparedStatement ps = null;
            try
            {
                ps = dialect.prepareQuery(con, "UPDATE {table} SET cMarkGarbage=? WHERE cParent=? AND cName=?");
                setPrimaryKey(ps, this, 1);
                ps.setLong(1, time);

                int count = ps.executeUpdate();
                if (count != 1)
                {
                    if (count == 0)
                    {
                        ps.clearWarnings(); // Derby generates warnings on no-match: https://issues.apache.org/jira/browse/DERBY-448
                        throw new SQLException("Critical consistency problem, no record found to mark for name=" + getName());
                    }
                    else
                    {
                        throw new SQLException("Critical consistency problem, more than one match while marking. count=" + count +" name=" + getName());
                    }
                }

                con.commit();
                dialect.returnConnection(con); con = null;

                // markTime is not stored in this object, so not updated here

                return;
            }
            catch (SQLException e)
            {
                if (!dialect.shouldRetry(e, retry))
                {
                    throw e;
                }
            }
            finally
            {
                rollbackConnection(null, null, ps, con);
            }
        } // retry
    }

    private long getMarkTime() throws SQLException, FileSystemException
    {
        // markTime is not read and stored by doAttach, so read it explicitely
        for(int retry=0; ; retry++)
        {
            long markTime = 0;
            PreparedStatement ps = null;
            ResultSet rs = null;
            Connection connection = getConnection("mark");
            try
            {
                ps = dialect.prepareQuery(connection, "SELECT cMarkGarbage FROM {table} WHERE cParent=? AND cName=?");
                setPrimaryKey(ps, this, 0);

                rs = ps.executeQuery();
                if (rs.next())
                {
                    markTime = rs.getLong(1);
                }

                return markTime;
            }
            catch (SQLException e)
            {
                if (!dialect.shouldRetry(e, retry))
                {
                    throw e;
                }
            }
            finally
            {
                closeConnection(null, rs, ps, connection);
            }
        } // retry
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
            catch (AbstractMethodError ignored) { /* nothing to recover */ } // TODO: JTDS
            catch (Exception ignored) { /* nothing to recover */ }
        }

        if (rs != null)
        {
            processWarnings(rs);
            try
            {
                rs.close();
            }
            catch (Exception ignored) { /* nothing to recover */ }
        }

        if (ps != null)
        {
            processWarnings(ps);
            try
            {
                ps.close();
            }
            catch (Exception ignored) { /* nothing to recover */ }
        }

        if (connection != null)
        {
            processWarnings(connection);
            dialect.returnConnection(connection);
        }
    }

    private void processWarnings(ResultSet rs)
    {
        try
        {
            processWarnings(rs.getWarnings());
        }
        catch (Exception ignored) { /* nothing to recover */ } // Oracle Driver NPE Problem
    }

    private void processWarnings(Connection connection)
    {
        try
        {
            processWarnings(connection.getWarnings());
        }
        catch (Exception ignored) { /* nothing to recover */ }
    }

    private void processWarnings(PreparedStatement ps)
    {
        try
        {
            processWarnings(ps.getWarnings());
        }
        catch (Exception ignored) { /* nothing to recover */ }
    }

    private void processWarnings(SQLWarning warnings)
    {
        if (warnings != null)
        {
            RuntimeException stack = new RuntimeException("Found JDBC Warnings: " + warnings);
            stack.fillInStackTrace();
            stack.printStackTrace(System.err); // TODO: logging
        }
    }

    private void rollbackConnection(Blob blob, ResultSet rs, PreparedStatement ps, Connection connection)
    {
        closeConnection(blob, rs, ps, null);

        if (connection != null)
        {
            processWarnings(connection);

            try
            {
                connection.rollback();
            }
            catch (Exception e)
            {
                e.printStackTrace(System.err);
            }
            dialect.returnConnection(connection);
        }
    }
}
