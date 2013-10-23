/*
 * DarcFileInputStreamTest.java
 *
 * created at 2013-09-16 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import org.junit.Ignore;
import org.junit.Test;


public class DarcFileInputStreamTest
{
    final static byte[] emptyBuf = getHex("789c4bcac94f523060000009b001f0"); // Git produced 0 byte blob

    @Test @Ignore
    public void testRead()
    {
        fail("Not yet implemented");
    }

    @Test @Ignore
    public void testReadByteArrayIntInt()
    {
        fail("Not yet implemented");
    }

    @Test @Ignore
    public void testSkip()
    {
        fail("Not yet implemented");
    }

    @Test  @Ignore
    public void testAvailable() throws IOException, NoSuchAlgorithmException
    {
        InputStream stream = emptyBlobStream();
        assertEquals(0, stream.available());
        stream.close();
    }

    @Test @Ignore
    public void testMark()
    {
        fail("Not yet implemented");
    }

    @Test @Ignore
    public void testReset()
    {
        fail("Not yet implemented");
    }

    @Test
    public void testMarkSupported() throws NoSuchAlgorithmException, IOException
    {
        InputStream stream = emptyBlobStream();
        assertFalse(stream.markSupported());
        stream.close();
    }


    @Test @Ignore
    public void testReadByteArray()
    {
        fail("Not yet implemented");
    }


    @Test
    public void testToString() throws NoSuchAlgorithmException, IOException
    {
        InputStream stream = emptyBlobStream();
        assertNotNull(stream.toString());
        stream.close();
    }

    @Test
    public void testReadFile0() throws IOException, NoSuchAlgorithmException
    {
        InputStream stream = emptyBlobStream();
        assertEquals(-1, stream.read());
        assertEquals(0, stream.available());
        stream.close();
    }

    @Test
    public void testReadAfterEnd() throws IOException, NoSuchAlgorithmException
    {
        InputStream stream = emptyBlobStream();
        assertEquals(-1, stream.read());
        assertEquals(0, stream.available());
        assertEquals(-1, stream.read());
        assertEquals(0, stream.available());
        stream.close();
    }



    private static byte[] getHex(String hexString)
    {
        byte[] bytes = new BigInteger(hexString, 16).toByteArray();
        byte[] adjusted = new byte[hexString.length()/2];
        System.arraycopy(bytes, bytes.length - adjusted.length, adjusted, 0, adjusted.length);
        return adjusted;
    }

    private DarcFileInputStream emptyBlobStream() throws IOException
    {
        ByteArrayInputStream bis = new ByteArrayInputStream(emptyBuf);
        return new DarcFileInputStream(bis, "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391");
    }

}
