/*
 * ObjectStorageTest.java
 *
 * created at 2013-09-16 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved. TODO
 */
package com.seeburger.vfs2.provider.digestarc;


import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import org.junit.Ignore;
import org.junit.Test;


public class ObjectStorageTest
{
    @Test
    public void testWriteBytes() throws NoSuchAlgorithmException, IOException
    {
        byte[] testBytes = "what is up, doc?".getBytes("ASCII");
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        ObjectStorage store = new ObjectStorage();
        byte[] digest = store.writeBytes(destination, testBytes, "blob");
        assertEquals("bd9dbf5aae1a3862dd1526723246b20206e5fc37", new BigInteger(1, digest).toString(16)); // testvector: http://git-scm.com/book/ch9-2.html

        testBytes = "test content\n".getBytes("ASCII");
        destination = new ByteArrayOutputStream();
        digest = store.writeBytes(destination, testBytes, "blob");
        assertEquals("d670460b4b4aece5915caf5c68d12f560a9fe3e4", new BigInteger(1, digest).toString(16)); // testvector: http://git-scm.com/book/en/Git-Internals-Git-Objects
    }

    @Test
    public void testWriteStream() throws NoSuchAlgorithmException, IOException
    {
        ObjectStorage store = new ObjectStorage();
        byte[] testBytes = "what is up, doc?".getBytes("ASCII");
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        ByteArrayInputStream source = new ByteArrayInputStream(testBytes);
        byte[] digest = store.writeStream(destination, source, testBytes.length, "blob");
        assertEquals("bd9dbf5aae1a3862dd1526723246b20206e5fc37", new BigInteger(1, digest).toString(16)); // testvector: http://git-scm.com/book/ch9-2.html

        testBytes = "test content\n".getBytes("ASCII");
        destination = new ByteArrayOutputStream();
        source = new ByteArrayInputStream(testBytes);
        digest = store.writeStream(destination, source, testBytes.length, "blob");
        assertEquals("d670460b4b4aece5915caf5c68d12f560a9fe3e4", new BigInteger(1, digest).toString(16)); // testvector: http://git-scm.com/book/en/Git-Internals-Git-Objects

    }

    @Test
    public void testWriteBytes0() throws NoSuchAlgorithmException, IOException
    {
        byte[] testBytes = new byte[0];
        ObjectStorage store = new ObjectStorage();

        ByteArrayOutputStream destination = new ByteArrayOutputStream();

        byte[] digest = store.writeBytes(destination, testBytes, "blob");
        assertEquals("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", new BigInteger(1, digest).toString(16)); // well known emtpy file
    }


    @Test
    public void testWriteStream0() throws NoSuchAlgorithmException, IOException
    {
        byte[] testBytes = new byte[0];
        ObjectStorage store = new ObjectStorage();

        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        ByteArrayInputStream source = new ByteArrayInputStream(testBytes);
        byte[] digest = store.writeStream(destination, source, testBytes.length, "blob");
        assertEquals("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", new BigInteger(1, digest).toString(16)); // well known emtpy file
    }

    @Test
    public void testWriteStream1M() throws NoSuchAlgorithmException, IOException
    {
        byte[] testBytes = new byte[1 * 1024 * 1024]; // all nul
        ObjectStorage store = new ObjectStorage();

        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        ByteArrayInputStream source = new ByteArrayInputStream(testBytes);
        byte[] digest = store.writeStream(destination, source, testBytes.length, "blob");
        assertEquals("9e0f96a2a253b173cb45b41868209a5d043e1437", new BigInteger(1, digest).toString(16));
    }

    @Test @Ignore
    public void testReadFromStream()
    {
        fail("Not yet implemented");
    }
}



