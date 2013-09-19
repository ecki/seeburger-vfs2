package com.seeburger.vfs2.provider.jdbctable;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class JdbcTableOutputStream extends OutputStream
{
    protected JdbcTableRowFile file;
    protected byte[] buffer1 = new byte[1];
    protected boolean closed = false;

    private IOException exc;
    private ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
    private boolean append;

    public JdbcTableOutputStream(JdbcTableRowFile file, boolean append) throws IOException
    {
        this.file = file;
        this.append = append;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        bufferStream.write(b, off,  len);
    }

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
            throw exc;
        }

        this.closed = true; // TODO - after Exception?
        try
        {
            this.file.writeData(bufferStream.toByteArray(), append);
            // writeData calls endOutput
        }
        catch (Exception e)
        {
            throw new IOException(e);
        }
    }
}
