/*
 * JdbcTableOutputStream.java
 *
 * created at 2013-08-10 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Output stream which reads all data into memory and
 * writes it to the {@link JdbcTableRowFile} on close.
 * <P>
 * WARNING: this can consume MAX_INTEGER bytes memory.
 *
 * @see ByteArrayOutputStream
 * @see JdbcTableRowFile#doGetOutputStream(boolean)
 */
public class JdbcTableOutputStream extends OutputStream
{
    protected final JdbcTableRowFile file;
    protected final boolean append;

    protected boolean closed = false;
    protected ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();


    public JdbcTableOutputStream(JdbcTableRowFile file, boolean append) throws IOException
    {
        this.file = file; // TODO: use data description instead
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
    public void close() throws IOException
    {
        if (closed)
        {
            return;
        }

        this.closed = true; // TODO - after write?
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
