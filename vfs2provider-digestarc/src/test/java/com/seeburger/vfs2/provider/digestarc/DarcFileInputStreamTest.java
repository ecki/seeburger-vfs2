/*
 * DarcFileInputStreamTest.java
 *
 * created at 2013-09-14 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved. TODO
 */
package com.seeburger.vfs2.provider.digestarc;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import org.junit.Ignore;
import org.junit.Test;


public class DarcFileInputStreamTest
{
    final static byte[] emptyBuf = getHex("789c4bcac94f523060000009b001f0"); // Git produced 0 byte

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
        ByteArrayInputStream bis = new ByteArrayInputStream(emptyBuf);
        DarcFileInputStream bla = new DarcFileInputStream(bis, "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391");
        assertEquals(0, bla.available());
        bla.close();
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
        ByteArrayInputStream bis = new ByteArrayInputStream(emptyBuf);
        DarcFileInputStream bla = new DarcFileInputStream(bis, "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391");
        assertFalse(bla.markSupported());
    }


    @Test @Ignore
    public void testReadByteArray()
    {
        fail("Not yet implemented");
    }


    @Test
    public void testToString() throws NoSuchAlgorithmException, IOException
    {
        ByteArrayInputStream bis = new ByteArrayInputStream(emptyBuf);
        DarcFileInputStream bla = new DarcFileInputStream(bis, "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391");
        assertNotNull(bla.toString());
        bla.close();
    }

    @Test
    public void testReadFile() throws IOException, NoSuchAlgorithmException
    {
        ByteArrayInputStream bis = new ByteArrayInputStream(emptyBuf);
        DarcFileInputStream bla = new DarcFileInputStream(bis, "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391");
        assertEquals(-1, bla.read());
        bla.close();
    }


    private static byte[] getHex(String hexString)
    {
        byte[] bytes = new BigInteger(hexString, 16).toByteArray();
        byte[] adjusted = new byte[hexString.length()/2];
        System.arraycopy(bytes, bytes.length - adjusted.length, adjusted, 0, adjusted.length);
        return adjusted;
    }

}
