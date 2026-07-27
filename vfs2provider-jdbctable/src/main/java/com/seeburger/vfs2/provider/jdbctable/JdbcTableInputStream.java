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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.seeburger.vfs2.provider.jdbctable.JdbcTableRowFile.DataDescription;


/**
 * Input Stream backed by a JDBC blob that transparently loads data in chunks of at most
 * {@link #MAX_BUFFER_SIZE} bytes, supporting files of arbitrary size.
 *
 * @see JdbcTableRowFile#startReadData(int)
 * @see JdbcTableRowFile#nextReadData(DataDescription, int)
 * @see JdbcTableRowFile#doGetInputStream()
 */
public class JdbcTableInputStream extends InputStream
{
    private static final Log LOG = LogFactory.getLog(JdbcTableInputStream.class);

    static final int MAX_BUFFER_SIZE = 50 * 1024 * 1024;

    /**
     * Abstracts next-chunk loading so the stream can be tested without a real database.
     * Implementations must return a byte array of up to {@code maxLen} bytes read from
     * the backing store starting at absolute file position {@code pos}.
     */
    interface ChunkLoader
    {
        byte[] load(long pos, int maxLen) throws IOException;
    }

    /** Keep state for re-requesting additional data (position, generation, total length). */
    final DataDescription dataDescription;

    /** Loads the next chunk from the backing store when the current buffer is exhausted. */
    private final ChunkLoader chunkLoader;

    /** position for mark/reset support. */
    long mark = 0;

    /** read position in the buffer */
    int bufferPos;
    /** Number of bytes in the buffer. */
    int bufferSize;
    /** Current data chunk buffer. Only {@link #bufferSize} bytes are valid. */
    byte[] buf;

    protected JdbcTableInputStream(JdbcTableRowFile file) throws IOException
    {
        long fileSize = file.getContent().getSize();
        int bufsize = (fileSize > MAX_BUFFER_SIZE) ? MAX_BUFFER_SIZE : (int) fileSize;

        // This is used to keep optimistic locking state.
        dataDescription = file.startReadData(bufsize);
        buf = dataDescription.buffer;
        bufferPos = 0;
        bufferSize = buf.length;

        if (LOG.isDebugEnabled() && fileSize > MAX_BUFFER_SIZE)
        {
            LOG.debug("JdbcTableInputStream on " + file + " will load " + fileSize
                      + " bytes in chunks of " + MAX_BUFFER_SIZE);
        }

        this.chunkLoader = (pos, maxLen) -> {
            dataDescription.pos = pos;
            file.nextReadData(dataDescription, maxLen);
            return dataDescription.buffer;
        };
    }

    /**
     * Loads the next chunk from the backing store into {@link #buf}.
     * Updates {@code dataDescription.pos}, {@link #bufferPos}, and {@link #bufferSize}.
     *
     * @return {@code true} if at least one byte was loaded; {@code false} at EOF
     * @throws IOException if the backing store signals an error or detects a content change
     */
    private boolean loadNextChunk() throws IOException
    {
        long nextPos = dataDescription.pos + bufferSize;
        if (nextPos >= dataDescription.dataLength)
        {
            return false;
        }
        int maxLen = (int) Math.min(MAX_BUFFER_SIZE, dataDescription.dataLength - nextPos);
        buf = chunkLoader.load(nextPos, maxLen);
        dataDescription.pos = nextPos;
        bufferPos = 0;
        bufferSize = buf.length;
        return bufferSize > 0;
    }

    /**
     * Reads the next byte of data from this input stream.
     */
    public synchronized int read() throws IOException
    {
        if (bufferPos >= bufferSize)
        {
            if (!loadNextChunk())
            {
                return -1;
            }
        }

        return (buf[bufferPos++] & 0xff);
    }

    /**
     * Reads up to <code>len</code> bytes of data into an array of bytes
     * from this input stream.
     */
    public synchronized int read(byte b[], int off, int len) throws IOException
    {
        if (off < 0 || len < 0 || len > b.length - off)
        {
            throw new IndexOutOfBoundsException();
        }

        if (len == 0)
        {
            return 0;
        }

        if (bufferPos >= bufferSize)
        {
            if (!loadNextChunk())
            {
                return -1;
            }
        }

        int readBytes = Math.min(len, bufferSize - bufferPos);
        System.arraycopy(buf, bufferPos, b, off, readBytes);
        bufferPos += readBytes;
        return readBytes;
    }

    /**
     * Skips <code>n</code> bytes of input from this input stream.
     * Transparently advances across chunk boundaries.
     */
    public synchronized long skip(long n) throws IOException
    {
        if (n <= 0)
        {
            return 0;
        }

        long remaining = n;
        while (remaining > 0)
        {
            int inBuffer = bufferSize - bufferPos;
            if (inBuffer > 0)
            {
                long skipNow = Math.min(remaining, inBuffer);
                bufferPos += (int) skipNow;
                remaining -= skipNow;
            }
            else
            {
                if (!loadNextChunk())
                {
                    break;
                }
            }
        }

        return n - remaining;
    }

    /**
     * Returns the total number of bytes that can still be read (or skipped) from this
     * stream, spanning all remaining chunks.
     */
    public synchronized int available()
    {
        long totalRemaining = dataDescription.dataLength - dataDescription.pos - bufferPos;
        return (int) Math.min(totalRemaining, Integer.MAX_VALUE);
    }

    /**
     * mark/reset is supported within the current chunk only.
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
        bufferPos = (int) mark; // TODO: support mark across chunk boundaries
    }

    public void close() throws IOException
    {
        buf = null;
    }
}
