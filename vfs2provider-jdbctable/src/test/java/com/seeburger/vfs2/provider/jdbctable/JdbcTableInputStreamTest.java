/*
 * JdbcTableInputStreamTest.java
 *
 * created at 2026-06-18
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable;


import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.vfs2.FileContent;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import com.seeburger.vfs2.provider.jdbctable.JdbcTableRowFile.DataDescription;


/**
 * Tests for {@link JdbcTableInputStream} covering streams backed by files larger than
 * {@link JdbcTableInputStream#MAX_BUFFER_SIZE} (50 MB).
 *
 * <p>Mockito mocks {@link JdbcTableRowFile} so that the <em>production</em>
 * {@code protected JdbcTableInputStream(JdbcTableRowFile)} constructor is exercised directly,
 * with no package-private test seam required in the production class.
 */
public class JdbcTableInputStreamTest
{
    /**
     * A stream over a file larger than {@code MAX_BUFFER_SIZE} must deliver all bytes,
     * not just the first {@code MAX_BUFFER_SIZE} bytes.
     */
    @Test
    public void testReadBulkStopsAtBufferBoundaryForLargeFile() throws Exception
    {
        int chunkSize  = 100;           // simulates MAX_BUFFER_SIZE
        int actualSize = chunkSize + 1; // one byte beyond first chunk

        JdbcTableInputStream stream = buildStream(makeData(actualSize), chunkSize);

        byte[] dest = new byte[actualSize];
        int totalRead = 0;
        int n;
        while ((n = stream.read(dest, totalRead, dest.length - totalRead)) > 0)
        {
            totalRead += n;
        }

        assertEquals("All bytes of a large file must be readable, not just the first buffer-full",
                     actualSize, totalRead);
    }

    /**
     * Single-byte {@code read()} must not stop at the buffer boundary for large files.
     */
    @Test
    public void testReadSingleByteStopsAtBufferBoundaryForLargeFile() throws Exception
    {
        int chunkSize  = 10;
        int actualSize = chunkSize + 1;

        JdbcTableInputStream stream = buildStream(makeData(actualSize), chunkSize);

        int consumed = 0;
        while (stream.read() != -1)
        {
            consumed++;
        }

        assertEquals("Single-byte read() must deliver all bytes of a large file",
                     actualSize, consumed);
    }

    /**
     * {@code skip(n)} must advance past the first buffer chunk when {@code n} exceeds it.
     */
    @Test
    public void testSkipClipsAtBufferBoundaryForLargeFile() throws Exception
    {
        int  chunkSize     = 100;
        int  actualSize    = chunkSize + 50;
        long requestedSkip = chunkSize + 25L;

        JdbcTableInputStream stream = buildStream(makeData(actualSize), chunkSize);
        long actuallySkipped = stream.skip(requestedSkip);

        assertEquals("skip() must be able to advance past the first buffer chunk",
                     requestedSkip, actuallySkipped);
    }

    /**
     * {@code available()} must not drop to zero at the buffer boundary when the file
     * still has more data.
     */
    @Test
    public void testAvailableIsZeroAfterBufferExhaustedAlthoughFileHasMoreData() throws Exception
    {
        int chunkSize  = 64;
        int actualSize = chunkSize + 1;

        JdbcTableInputStream stream = buildStream(makeData(actualSize), chunkSize);

        byte[] sink = new byte[chunkSize];
        int read = stream.read(sink, 0, chunkSize);
        assertEquals(chunkSize, read);

        assertTrue("available() must be > 0 when file has more data beyond the first buffer",
                   stream.available() > 0);
    }

    /**
     * After a chunk-load failure the stream must keep its previous position so a subsequent
     * retry computes the same next chunk and can continue reading.
     */
    @Test
    public void testReadRetriesSameChunkAfterNextReadDataIOException() throws Exception
    {
        byte[] fullData = makeData(12);
        int chunkSize = 4;

        FileContent content = mock(FileContent.class);
        when(content.getSize()).thenReturn((long) fullData.length);

        JdbcTableRowFile file = mock(JdbcTableRowFile.class);
        when(file.getContent()).thenReturn(content);

        DataDescription startDesc = new DataDescription();
        startDesc.dataLength = fullData.length;
        startDesc.pos = 0;
        startDesc.buffer = Arrays.copyOf(fullData, chunkSize);
        when(file.startReadData(anyInt())).thenReturn(startDesc);

        final int[] nextReadCalls = { 0 };
        doAnswer((InvocationOnMock inv) -> {
            DataDescription desc = inv.getArgument(0);
            int maxLen = inv.getArgument(1);
            nextReadCalls[0]++;
            if (nextReadCalls[0] == 1)
            {
                throw new IOException("simulated DB read failure");
            }
            int len = (int) Math.min(Math.min(maxLen, chunkSize), fullData.length - desc.pos);
            desc.buffer = Arrays.copyOfRange(fullData, (int) desc.pos, (int) desc.pos + len);
            return null;
        }).when(file).nextReadData(any(DataDescription.class), anyInt());

        JdbcTableInputStream stream = new JdbcTableInputStream(file);

        byte[] firstChunk = new byte[chunkSize];
        assertEquals(chunkSize, stream.read(firstChunk, 0, chunkSize));

        try
        {
            stream.read();
            fail("Expected IOException from nextReadData");
        }
        catch (IOException expected)
        {
            assertEquals("dataDescription.pos must remain at previous chunk after failure",
                         0L, startDesc.pos);
        }

        assertEquals("Retry must continue from the same chunk position",
                     fullData[chunkSize] & 0xff, stream.read());
        assertEquals("Successful retry must move to the requested chunk start",
                     (long) chunkSize, startDesc.pos);
        verify(file, times(2)).nextReadData(any(DataDescription.class), anyInt());
    }

    @Test
    public void testResetFailsAfterCrossingChunkBoundary() throws Exception
    {
        int chunkSize = 4;
        JdbcTableInputStream stream = buildStream(makeData(chunkSize + 2), chunkSize);

        stream.mark(0);
        byte[] firstChunk = new byte[chunkSize];
        assertEquals(chunkSize, stream.read(firstChunk, 0, firstChunk.length));
        assertEquals("Cross chunk to make mark invalid", chunkSize + 1, stream.read());

        try
        {
            stream.reset();
            fail("Expected reset to fail after moving to another chunk");
        }
        catch (IOException expected)
        {
            // expected
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Builds recognisable test data: bytes cycle 1..127 so values are never zero. */
    private static byte[] makeData(int size)
    {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++)
            data[i] = (byte) (i % 127 + 1);
        return data;
    }

    /**
     * Creates a {@link JdbcTableInputStream} via the <em>real</em> production constructor
     * by mocking {@link JdbcTableRowFile}.
     *
     * <ul>
     *   <li>{@code getContent().getSize()} returns {@code fullData.length}.</li>
     *   <li>{@code startReadData(n)} returns a {@link DataDescription} pre-loaded with
     *       the first {@code chunkSize} bytes.</li>
     *   <li>{@code nextReadData(desc, n)} fills {@code desc.buffer} with the next slice,
     *       exactly as the real JDBC implementation does.</li>
     * </ul>
     *
     * @param fullData  complete file content
     * @param chunkSize maximum bytes per chunk (analogous to MAX_BUFFER_SIZE)
     */
    private static JdbcTableInputStream buildStream(byte[] fullData, int chunkSize) throws Exception
    {
        FileContent content = mock(FileContent.class);
        when(content.getSize()).thenReturn((long) fullData.length);

        JdbcTableRowFile file = mock(JdbcTableRowFile.class);
        when(file.getContent()).thenReturn(content);

        // startReadData: return first chunk eagerly (mirrors real implementation)
        when(file.startReadData(anyInt())).thenAnswer((InvocationOnMock inv) -> {
            DataDescription desc = new DataDescription();
            desc.dataLength = fullData.length;
            desc.pos        = 0;
            desc.buffer     = Arrays.copyOf(fullData, chunkSize);
            return desc;
        });

        // nextReadData: fill desc.buffer with the next slice in-place (mirrors real implementation)
        doAnswer((InvocationOnMock inv) -> {
            DataDescription desc   = inv.getArgument(0);
            int             maxLen = inv.getArgument(1);
            int             len    = (int) Math.min(Math.min(maxLen, chunkSize), fullData.length - desc.pos);
            desc.buffer = Arrays.copyOfRange(fullData, (int) desc.pos, (int) desc.pos + len);
            return null;
        }).when(file).nextReadData(any(DataDescription.class), anyInt());

        return new JdbcTableInputStream(file);
    }
}
