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


/**
 * Input Stream backed by a JDBC blob.
 * <P>
 * WARNING: this currently only allows to read blobs up to 50 MB.
 * This is a TODO, it is intended to reopen the Blob every {@link #MAX_BUFFER_SIZE}
 * read bytes.
 *
 * @see JdbcTableRowFile#startReadData(int)
 * @see JdbcTableRowFile#doGetInputStream()
 */
public class JdbcTableInputStream extends InputStream
{
    static final int MAX_BUFFER_SIZE = 50 * 1024 * 1024;

    /** Keep state for re-requesting additional data. */
    final DataDescription dataDescription;

    /** position for mark/reset support. */
    long mark = 0;

    /** read position in the buffer */
    int bufferPos;
    /** Number of bytes in the buffer. */
    int bufferSize;
    /** Current data chunk buffer. Only {@link #bufferSize} bytes are valid. */
    byte[] buf;

    protected JdbcTableInputStream(JdbcTableRowFile file) throws IOException, SQLException
    {
        long fileSize = file.getContent().getSize();
        int bufsize;

        if (fileSize > (long)MAX_BUFFER_SIZE)
        {
            bufsize = MAX_BUFFER_SIZE;
            System.out.println("TODO: Warning, JdbcTableInputStream on " + file  + " only supports " + MAX_BUFFER_SIZE + " bytes. (filesize=" + fileSize + ")");
        }
        else
        {
            bufsize = (int)fileSize;
        }

        // This is used to keep optimistic lockig state.
        dataDescription = file.startReadData(bufsize);
        buf = dataDescription.buffer;
        bufferPos = 0;
        bufferSize = buf.length;
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
        if (off < 0 || len < 0 || len > b.length - off)
        {
            throw new IndexOutOfBoundsException();
        }

        if (bufferPos >= bufferSize)
        {
            return -1;
        }

        int readBytes;
        if (bufferPos + len > bufferSize)
        {
            readBytes = bufferSize - bufferPos;
        } else {
            readBytes = len;
        }

        if (readBytes <= 0) // == 0
        {
            return 0;
        }

        System.arraycopy(buf, bufferPos, b, off, readBytes);
        bufferPos += readBytes;
        return readBytes;
    }

    /**
     * Skips <code>n</code> bytes of input from this input stream.
     */
    public synchronized long skip(long n)
    {
        int skipBytes;

        if (bufferPos + n > bufferSize)
        {
            skipBytes = bufferSize - bufferPos;
        }
        else if (n > Integer.MAX_VALUE)
        {
            skipBytes = Integer.MAX_VALUE;
        }
        else
        {
            skipBytes = (int)n;
        }

        if (skipBytes < 0)
        {
            return 0;
        }
        bufferPos += skipBytes;
        return skipBytes;
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
    public synchronized void mark(int readAheadLimit)
    {
        mark = bufferPos;
    }

    /**
     * Resets the buffer to the marked position.
     */
    public synchronized void reset()
    {
        bufferPos = (int)mark; // TODO: relative to buffer
    }

    public void close() throws IOException
    {
        buf = null;
    }
}
