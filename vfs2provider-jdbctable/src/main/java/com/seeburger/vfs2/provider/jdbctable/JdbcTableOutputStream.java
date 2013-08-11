package com.seeburger.vfs2.provider.jdbctable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.ram.RamFileObject;

public class JdbcTableOutputStream extends OutputStream
{
    protected JdbcTableRowFile file;
    protected byte[] buffer1 = new byte[1];
    protected boolean closed = false;
    private IOException exc;
    private ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();

    public JdbcTableOutputStream(JdbcTableRowFile file, boolean append)
    {
        this.file = file;
    	if (append)
    		throw new RuntimeException("Appending not supported.");
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
    	bufferStream.write(b, off,  len);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.DataOutput#write(int)
     */
    @Override
    public void write(int b) throws IOException
    {
    	bufferStream.write(b);
    }

    @Override
    public void flush() throws IOException
    {
    }

    @Override
    public void close() throws IOException
    {
        if (closed)
        {
            return;
        }
        // Notify on close that there was an IOException while writing
        if (exc != null)
        {
        	// TODO: discard?
            throw exc;
        }
        try
        {
            this.closed = true; // TODO
            this.file.writeData(bufferStream.toByteArray());
        }
        catch (Exception e)
        {
            throw new FileSystemException(e);
        }
    }

}
