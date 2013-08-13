/*
 * JdbcTableInputStream.java
 *
 * created at 2013-08-12 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import com.seeburger.vfs2.provider.jdbctable.JdbcTableRowFile.DataDescription;


public class JdbcTableInputStream extends InputStream
{
    static final long MAX_BUFFER_SIZE = 2 * 1024 * 1024;

    long mark = 0; // TODO: long
    JdbcTableRowFile file;

    DataDescription dataDescription;

    int bufferPos;
    int bufferSize;
    byte[] buf;

    protected JdbcTableInputStream(JdbcTableRowFile file) throws IOException, SQLException
    {
        this.file = file;
        int bufsize = (int)Math.min(MAX_BUFFER_SIZE, file.getContent().getSize());

        dataDescription = file.startReadData(bufsize);
        buf = dataDescription.buffer;
        bufferPos = 0;
        bufferSize = buf.length;
        mark = 0;
    }

    /**
     * Reads the next byte of data from this input stream.
     */
    public synchronized int read()
    {
        if (bufferPos >= bufferSize)
        {
            return -1;
        }

        return (buf[bufferPos++] & 0xff);
    }

    /**
     * Reads up to <code>len</code> bytes of data into an array of bytes
     * from this input stream.
     */
    public synchronized int read(byte b[], int off, int len)
    {
        if (b == null)
        {
            throw new NullPointerException();
        }
        else if (off < 0 || len < 0 || len > b.length - off)
        {
            throw new IndexOutOfBoundsException();
        }

        if (bufferPos >= bufferSize)
        {
            return -1;
        }

        if (bufferPos + len > bufferSize)
        {
            len = bufferSize - bufferPos;
        }

        if (len <= 0) // == 0
        {
            return 0;
        }

        System.arraycopy(buf, bufferPos, b, off, len);
        bufferPos += len;
        return len;
    }

    /**
     * Skips <code>n</code> bytes of input from this input stream.
     */
    public synchronized long skip(long n)
    {
        if (bufferPos + n > bufferSize)
        {
            n = bufferSize - bufferPos;
        }
        if (n < 0)
        {
            return 0;
        }
        bufferPos += n;
        return n;
    }

    /**
     * Returns the number of remaining bytes that can be read (or skipped over)
     * from this input stream.
     */
    public synchronized int available()
    {
        return bufferSize - bufferPos;
    }

    /**
     * mark/reset is supported.
     */
    public boolean markSupported()
    {
        return true;
    }

    /**
     * Set the current marked position in the stream.
     */
    public void mark(int readAheadLimit)
    {
        mark = bufferPos;
    }

    /**
     * Resets the buffer to the marked position.
     */
    public synchronized void reset()
    {
        bufferPos = (int)mark; // TODO: relative
    }

    public void close() throws IOException
    {
        dataDescription = null;
        buf = null;
    }
}



