/*
 * DarcTreeTest.java
 *
 * created at 2013-09-14 by Bernd Eckenfels <b.eckenfels@seeburger.de>
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
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.seeburger.vfs2.provider.digestarc.DarcTree.Directory;
import com.seeburger.vfs2.provider.digestarc.DarcTree.Entry;
import com.seeburger.vfs2.provider.digestarc.DarcTree.File;


public class DarcTreeTest
{
    @Test
    public void testDarcFileWriteBlob16() throws NoSuchAlgorithmException, IOException
    {
        DarcTree df = new DarcTree();
        File f = df.new File(0, "");

        byte[] testBytes = "what is up, doc?".getBytes("ASCII");
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        ByteArrayInputStream source = new ByteArrayInputStream(testBytes);
        byte[] digest = f.writeBlob(destination, testBytes.length, source);
        assertEquals("bd9dbf5aae1a3862dd1526723246b20206e5fc37", new BigInteger(1, digest).toString(16)); // testvector: http://git-scm.com/book/ch9-2.html

        testBytes = "test content\n".getBytes("ASCII");
        destination = new ByteArrayOutputStream();
        source = new ByteArrayInputStream(testBytes);
        digest = f.writeBlob(destination, testBytes.length, source);
        assertEquals("d670460b4b4aece5915caf5c68d12f560a9fe3e4", new BigInteger(1, digest).toString(16)); // testvector: http://git-scm.com/book/en/Git-Internals-Git-Objects
    }

    @Test
    public void testDarcFileWriteBlob0() throws NoSuchAlgorithmException, IOException
    {
        byte[] testBytes = new byte[0];
        DarcTree df = new DarcTree();
        File f = df.new File(0, "");

        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        ByteArrayInputStream source = new ByteArrayInputStream(testBytes);

        byte[] digest = f.writeBlob(destination, testBytes.length, source);
        assertEquals("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", new BigInteger(1, digest).toString(16)); // well known emtpy file
    }

    @Test
    public void testDarcFileWriteBlob1M() throws NoSuchAlgorithmException, IOException
    {
        byte[] testBytes = new byte[1 * 1024 * 1024];
        DarcTree df = new DarcTree();
        File f = df.new File(0, "");

        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        ByteArrayInputStream source = new ByteArrayInputStream(testBytes);

        byte[] digest = f.writeBlob(destination, testBytes.length, source);
        assertEquals("9e0f96a2a253b173cb45b41868209a5d043e1437", new BigInteger(1, digest).toString(16));
    }

    @Test @Ignore
    public void testReadFromStream()
    {
        fail("Not yet implemented");
    }

    @Test(expected=IOException.class)
    public void testResolveFileNotFound() throws IOException
    {
        DarcTree tree = buildDefaultTestTree();
        tree.resolveName("/notfound", null);
    }

    @Test
    public void testResolveIsDirectory() throws IOException
    {
        DarcTree tree = buildDefaultTestTree();
        Entry result = tree.resolveName("/dir1", null);
        assertTrue("Must be a Directory entry", result instanceof Directory);
    }

    @Test
    public void testResolveIsFile() throws IOException
    {
        DarcTree tree = buildDefaultTestTree();
        Entry result = tree.resolveName("/dir1/dir1file3", null);
        assertTrue("Must be a File entry", result instanceof File);
    }

    @Test(expected=IOException.class)
    public void testResolveTraverseFile() throws IOException
    {
        DarcTree tree = buildDefaultTestTree();
        Entry result = tree.resolveName("/file2/test", null);
        assertNotNull(result);
    }

    @Test(expected=IOException.class)
    public void testResolveDirectoryNotFound() throws IOException
    {
        DarcTree tree = buildDefaultTestTree();
        Entry result = tree.resolveName("/dir2/test", null);
        assertNotNull(result);
    }

    /** this constructs a small entirely self defined directory tree. */
    private DarcTree buildDefaultTestTree()
    {
        DarcTree df = new DarcTree();
        File file = df.new File(0, "abc");
        Map<String, DarcTree.Entry> content = new HashMap<String, DarcTree.Entry>();
        content.put("dir1file3", file);
        Directory dir = df.new Directory(content);

        content = new HashMap<String, DarcTree.Entry>();
        content.put("file1", file);
        content.put("file2", file);
        content.put("dir1", dir);
        return new DarcTree(content);
    }

}



