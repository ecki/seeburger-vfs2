/*
 * DarcBasicTest.java
 *
 * created at 2013-10-15 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileNotFolderException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.cache.DefaultFilesCache;
import org.apache.commons.vfs2.impl.DefaultFileReplicator;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.LayeredFileName;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.apache.commons.vfs2.provider.ram.RamFileProvider;
import org.apache.commons.vfs2.provider.url.UrlFileProvider;
import org.junit.Ignore;
import org.junit.Test;

import com.seeburger.vfs2.operations.CollectFilesOperation;
import com.seeburger.vfs2.util.TreePrinter;


public class DarcBasicTest
{
    @Test(expected=FileSystemException.class)
    public void testReadOnlyRoot() throws IOException
    {
        FileObject root = getTestRoot(false);
        root.createFile();
    }

    @Test(expected=FileSystemException.class)
    public void testReadOnlyFile() throws IOException
    {
        FileObject root = getTestRoot(false);
        root.resolveFile("file").createFile();
    }

    @Test(expected=FileSystemException.class)
    public void testReadOnlyRootDir() throws IOException
    {
        FileObject root = getTestRoot(false);
        root.resolveFile("dir").createFolder();
    }

    @Test
    public void testRootFile() throws IOException
    {
        FileObject root = getTestRoot(true);
        FileObject file = root.resolveFile("file");
        assertEquals(FileType.IMAGINARY, file.getType());

        file.createFile();
        assertEquals(FileType.FILE, file.getType());
    }

    @Test
    public void testRootDir() throws IOException
    {
        FileObject root = getTestRoot(true);
        FileObject file = root.resolveFile("dir");
        assertEquals(FileType.IMAGINARY, file.getType());

        file.createFolder();
        assertEquals(FileType.FOLDER, file.getType());
    }

    @Test
    public void testParentChild() throws IOException
    {
        FileObject root = getTestRoot(true);

        FileObject dir = root.resolveFile("dir");
        assertEquals(FileType.IMAGINARY, dir.getType());

        FileObject file = root.resolveFile("dir/file");
        assertEquals(FileType.IMAGINARY, file.getType());

        file.createFile();
        assertEquals(FileType.FILE, file.getType());
        assertEquals(FileType.FOLDER, dir.getType());

        dir = root.resolveFile("dir");
        assertEquals(FileType.FOLDER, dir.getType());

        file = dir.getChild("file");
        assertEquals(FileType.FILE, file.getType());
    }

    @Test
    public void testCreateChildGap0() throws IOException
    {
        FileObject root = getTestRoot(true);

        FileObject file = root.resolveFile("dir1/fileA");
        assertEquals(FileType.IMAGINARY, file.getType());

        file.createFile();
        assertEquals(FileType.FILE, file.getType());

        file = root.resolveFile("dir1/fileA");
        assertEquals(FileType.FILE, file.getType());

        FileObject dir = root.resolveFile("dir1");
        file = dir.getChild("fileA");
        assertNotNull(file);
    }

    @Test
    public void testCreateChildGap1() throws IOException
    {
        FileObject root = getTestRoot(true);

        FileObject file = root.resolveFile("dir1/missing1/fileA");
        assertEquals(FileType.IMAGINARY, file.getType());

        file.createFile();
        assertEquals(FileType.FILE, file.getType());

        file = root.resolveFile("dir1/missing1/fileA");
        assertEquals(FileType.FILE, file.getType());
        assertEquals(FileType.FOLDER, file.getParent().getType());

        FileObject dir = root.resolveFile("dir1");
        file = dir.getChild("missing1");
        assertNotNull(file);
        file = file.getChild("fileA");
        assertNotNull(file);
    }

    @Test
    public void testCreateChildGap2() throws IOException
    {
        FileObject root = getTestRoot(true);

        FileObject file = root.resolveFile("dir1/missing1/missing2/fileA");
        assertEquals(FileType.IMAGINARY, file.getType());

        file.createFile();
        assertEquals(FileType.FILE, file.getType());

        file = root.resolveFile("dir1/missing1/missing2/fileA");
        assertEquals(FileType.FILE, file.getType());
        assertEquals(FileType.FOLDER, file.getParent().getType());

        FileObject dir = root.resolveFile("dir1");
        file = dir.getChild("missing1");
        assertNotNull(file);
        file = file.getChild("missing2");
        assertNotNull(file);
        file = file.getChild("fileA");
        assertNotNull(file);
    }

    @Test(expected=FileSystemException.class)
    public void testCreateOverlap() throws IOException
    {
        FileObject file1 = null;
        try
        {
            FileObject root = getTestRoot(true);

            file1 = root.resolveFile("dir2/missing1/fileA");
            assertEquals(FileType.IMAGINARY, file1.getType());

            FileObject file2 = root.resolveFile("dir2/missing1");
            assertEquals(FileType.IMAGINARY, file1.getType());

            file2.createFile();
            assertEquals(FileType.FILE, file2.getType());
        }
        catch (Exception failure) { fail(failure.toString()); }

        file1.createFile(); // must fail
    }

    @Test
    public void testResolveNotFolder() throws IOException
    {
        FileObject root = getTestRoot(true);
        FileObject file1 = root.resolveFile("dir1/dir1a/file1/file");
        try {
            file1.exists();
            fail("Expecting FSE when resolving file after file.");
        }
        catch (FileSystemException fse)
        {
            Throwable cause = fse.getCause();
            assertNotNull("Filesystem Exception " + fse + " must have a cause.", cause);
            assertSame(FileNotFolderException.class, cause.getClass());
        }
    }

    @Test
    public void testSpecialInName() throws IOException
    {
        FileObject root = getTestRoot(true);
        FileObject dir = root.resolveFile("special");
        dir.createFolder();
        // unfortunatelly ! cannot yet be used in file names, not even encoded.
        FileObject file1 = dir.resolveFile("https _ - + %25 ? () : test", NameScope.CHILD);
        file1.createFile();
        FileObject[] ls = dir.findFiles(new AllFileSelector());
        assertSame(2, ls.length);
        String p = ls[0].getName().getPathDecoded();
        assertTrue("(initial) The name is " + p + " does not correctly end", p.endsWith("/special/https _ - + % ? () : test"));

        // repeat to execise cached path as wenn:
        ls = dir.findFiles(new AllFileSelector());
        assertSame(2, ls.length);
        p = ls[0].getName().getPathDecoded();
        assertTrue("(cached) The name is " + p + " does not correctly end", p.endsWith("/special/https _ - + % ? () : test"));
    }


    @Test
    public void testParentNoticesDelete() throws IOException
    {
        FileObject root = getTestRoot(true);

        FileObject dir = root.resolveFile("dir1");
        assertEquals(FileType.FOLDER, dir.getType());

        dir = dir.getChild("dir1a");
        assertEquals(FileType.FOLDER, dir.getType());

        FileObject file = dir.getChild("file1");
        assertEquals(FileType.FILE, file.getType());

        file.delete();
        assertEquals(FileType.IMAGINARY, file.getType());

        file = dir.getChild("file1");
        assertNull(file);

        file = root.resolveFile("dir1/dir1a/file1");
        assertEquals(FileType.IMAGINARY, file.getType());
    }

    @Test @Ignore // rename not implemented
    public void testParentRename() throws IOException
    {
        FileObject root = getTestRoot(true);

        FileObject dir2 = root.resolveFile("dir1/dir1a");
        assertEquals(FileType.FOLDER, dir2.getType());

        FileObject dir1 = root.resolveFile("dir1");
        assertEquals(FileType.FOLDER, dir1.getType());

        FileObject dir3 = root.resolveFile("dir2/dir2c");
        assertEquals(FileType.IMAGINARY, dir3.getType());

        dir2.moveTo(dir3);

        dir1 = root.resolveFile("dir1/dir1a");
        assertEquals(FileType.IMAGINARY, dir1.getType());

        dir2 = root.resolveFile("dir2");
        assertEquals(FileType.FOLDER, dir1.getType());

        dir2 = dir2.getChild("dir2c");
        assertEquals(FileType.FOLDER, dir2.getType());

        dir3 = root.resolveFile("dir2/dir2c");
        assertEquals(FileType.FOLDER, dir3.getType());
    }



    /** creates a new test filesystem.
     * @throws IOException */
    private FileObject getTestRoot(boolean write) throws IOException
    {
        DefaultFileSystemManager manager = new DefaultFileSystemManager();
        manager.addProvider("file", new DefaultLocalFileProvider());
        manager.addProvider("ram", new RamFileProvider());
        manager.addProvider("darc", new DarcFileProvider());
        manager.addOperationProvider("darc", DarcFileOperationProvider.getInstance());

        manager.setCacheStrategy(CacheStrategy.MANUAL);
        manager.setFilesCache(new DefaultFilesCache());

        manager.setDefaultProvider(new UrlFileProvider());
        manager.setBaseFile(manager.resolveFile("ram:/"));

        File tmp = new File(System.getProperty("java.io.tmpdir"), "vfslocal");
        tmp.mkdirs();
        manager.setReplicator(new DefaultFileReplicator(tmp));

        FileSystemOptions writeOpts = new FileSystemOptions();
        DarcFileConfigBuilder config = DarcFileConfigBuilder.getInstance();
        config.setChangeSession(writeOpts, "+");
        FileObject root = manager.resolveFile("darc:ram:/", writeOpts);
        root.resolveFile("dir1").createFolder();
        root.resolveFile("dir1/dir1a").createFolder();
        root.resolveFile("dir1/dir1b").createFolder();
        root.resolveFile("dir1/dir1a/file1").createFile();
        root.resolveFile("dir1/dir1a/file2").createFile();
        root.resolveFile("dir2").createFolder();
        root.resolveFile("dir2/dir2a").createFolder();
        root.resolveFile("dir2/dir2a/file1").createFile();
        root.resolveFile("dir2/dir2a/file2").createFile();
        root.resolveFile("dir2/dir2b").createFolder();
        DarcFileSystem fs = (DarcFileSystem)root.getFileSystem();
        String id = fs.commitChanges();

        if (write)
            return manager.resolveFile("darc:ram:/" + BlobStorageProvider.hashToPath(id) + "!/", writeOpts);
        else
            return manager.resolveFile("darc:ram:/" + BlobStorageProvider.hashToPath(id) + "!/");
    }

    @Test
    public void testCreateRecreate() throws IOException
    {
        FileObject root = getTestRoot(true);
        FileObject file = root.resolveFile("file");
        assertEquals(FileType.IMAGINARY, file.getType());

        MyFileListener list = new MyFileListener();
        file.getFileSystem().addListener(file, list);
        try
        {
            file.createFile();
            assertEquals(FileType.FILE, file.getType());
            FileContent content = file.getContent();
            OutputStream os = content.getOutputStream();
            os.close();

            file.delete();

            assertTrue("One file should be deleted", 1 == list.deleted);
            assertTrue("One file should be created", 1 ==  list.created);
        }
        finally
        {
            file.getFileSystem().removeListener(file, list);
        }
    }


    @Test
    public void testResolveChild() throws IOException
    {
        FileObject root = getTestRoot(true);

        FileObject dir1 = root.resolveFile("dir1");
        assertEquals(FileType.FOLDER, dir1.getType());
        LayeredFileName dir1Name = (LayeredFileName)dir1.getName();

        FileObject dir2 = root.resolveFile("dir1/dir1a");
        assertEquals(FileType.FOLDER, dir2.getType());
        LayeredFileName dir2Name = (LayeredFileName)dir2.getName();

        assertEquals(dir1Name.getOuterName(), dir2Name.getOuterName());
        // TODO this would be nice: assertSame(dir1Name.getOuterName(), dir2Name.getOuterName());
    }

    @Test
    public void testCollectBlobs() throws IOException
    {
        FileObject root = getTestRoot(true);

        FileObject parent = root.resolveFile("dir2");

        CollectFilesOperation collect = (CollectFilesOperation)parent.getFileOperations().getOperation(CollectFilesOperation.class);
        ArrayList<String> list = new ArrayList<String>();

        collect.setFilesList(list);
        collect.process();

        TreePrinter.printTree(parent, "> ", System.out);

        // sequence is not guranteed (java8 is known to produce different from java 7)
        Collections.sort(list);

        // since we use an array list duplicates are not removed
        assertEquals("[ram:///91/4ce7f9885561e332f4cfe8b900507c3b30d4bd, ram:///95/702f71b2ac6b7ee67ff9f07eadfc9fb5dbac49, ram:///e6/9de29bb2d1d6434b8b29ae775ad8c2e48c5391, ram:///e6/9de29bb2d1d6434b8b29ae775ad8c2e48c5391, ram:///f2/5a65f492bc70b7db123964d374c5b953557a9d]", list.toString());
    }




    static class MyFileListener implements FileListener
    {
        public int deleted, created, changed;
        @Override
        public void fileDeleted(FileChangeEvent event)
            throws Exception
        {
            deleted++;
        }


        @Override
        public void fileCreated(FileChangeEvent event)
            throws Exception
        {
            created++;
        }


        @Override
        public void fileChanged(FileChangeEvent event)
            throws Exception
        {
            changed++;
        }
    }

}



