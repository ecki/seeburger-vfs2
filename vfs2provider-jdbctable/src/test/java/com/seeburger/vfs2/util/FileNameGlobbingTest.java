/*
 * FileNameGlobbingTest.java
 *
 * created at 2013-09-13 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.util;


import static org.junit.Assert.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.cache.DefaultFilesCache;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.junit.Ignore;
import org.junit.Test;


public class FileNameGlobbingTest
{
    @Test
    public void testFileNameGlobbingSimpleConstructor()
    {
        FileNameGlobbing g = new FileNameGlobbing("pattern");
        assertArrayEquals(new String[]{ "pattern" }, g.getExplodedPattern());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFileNameGlobbingNullConstructor()
    {
        new FileNameGlobbing(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFileNameGlobbingEmptyConstructor()
    {
        new FileNameGlobbing("");
    }

    @Test
    public void testCreatePath()
    {
        String[] empty = new String[] { };
        String[] five = new String[] { "1/", "2/", "3/", "4/", "5" };

        assertEquals("", FileNameGlobbing.createPath(empty, 0, 0));
        assertEquals("", FileNameGlobbing.createPath(five, 0, 0));
        assertEquals("1/", FileNameGlobbing.createPath(five, 0, 1));
        assertEquals("2/", FileNameGlobbing.createPath(five, 1, 2));
        assertEquals("1/2/", FileNameGlobbing.createPath(five, 0, 2));
        assertEquals("1/2/3/4/5", FileNameGlobbing.createPath(five, 0, 5));
        assertEquals("1/2/3/4/5", FileNameGlobbing.createPath(five, 0, 10));
    }


    @Test
    public void testGetBase()
    {
        assertBase("./", "./");
        assertBase("/", "/");
        assertBase("/file", "/file");
        assertBase("dir1/dir2/file", "dir1/dir2/file");
        assertBase("dir1/dir2/", "dir1/dir2/");
        assertBase("/test/", "/test/*/file");
        assertBase("/test/", "/test/test*test/dir");
    }

    @Test
    public void testGetExploded()
    {
        assertEquals(1, FileNameGlobbing.getExploded("1").length);
    }

    @Test
    public void testGetSelectorDirWild() throws FileSystemException
    {
        FileObject dir = getTestDir();
        FileNameGlobbing g = new FileNameGlobbing("test-data/*/dir1/*");

        // get a selector for this pattern
        FileSelector s = g.getSelector();
        assertNotNull(s);

        // use the selector to filter
        dir.refresh();
        FileObject[] files = dir.findFiles(s);
        assertNotNull(files);

        // construct set of relative names
        Set<String> names = new HashSet<String>();
        for(FileObject file : files)
            names.add(dir.getName().getRelativeName(file.getName()));

        assertTrue(names.remove("test-data/read-tests/dir1/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/file2.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/file3.txt"));
        assertTrue(names.isEmpty());
    }

    @Test
    public void testGetSelectorFileWild() throws FileSystemException
    {
        FileObject dir = getTestDir();
        FileNameGlobbing g = new FileNameGlobbing("test-data/*.tar");

        // get a selector for this pattern
        FileSelector s = g.getSelector();
        assertNotNull(s);

        // use the selector to filter
        dir.refresh();
        FileObject[] files = dir.findFiles(s);
        assertNotNull(files);

        // construct set of relative names
        Set<String> names = new HashSet<String>();
        for(FileObject file : files)
            names.add(dir.getName().getRelativeName(file.getName()));

        assertTrue(names.remove("test-data/nested.tar"));
        assertTrue(names.remove("test-data/test.tar"));
        assertTrue(names.isEmpty());
    }

    @Test @Ignore
    public void testGetSelectorDotStar() throws FileSystemException
    {
        FileObject dir = getTestDir();
        assertTrue("Test directory broken", dir.getChild("test-data").getChild("test-hash-#test.txt").exists());

        FileNameGlobbing g = new FileNameGlobbing("test-data/test.*");


        // get a selector for this pattern
        FileSelector s = g.getSelector();
        assertNotNull(s);

        // use the selector to filter
        dir.refresh();
        FileObject[] files = dir.findFiles(s);
        assertNotNull(files);

        // construct set of relative names
        Set<String> names = new HashSet<String>();
        for(FileObject file : files)
            names.add(dir.getName().getRelativeName(file.getName()));

        assertTrue(names.remove("test-data/test.mf"));
        assertTrue(names.remove("test-data/test.policy"));
        assertFalse("TODO: fix .* escape", names.remove("test-data/test-hash-#test.txt"));
        // will also match test-hash.txt if .* is escaped wrong
        assertTrue(names.isEmpty());
    }

    @Test
    public void testGetSelectorWithHash() throws FileSystemException
    {
        FileObject dir = getTestDir();
        FileNameGlobbing g = new FileNameGlobbing("test-data/test-hash-#*");

        // get a selector for this pattern
        FileSelector s = g.getSelector();
        assertNotNull(s);

        // use the selector to filter
        dir.refresh();
        FileObject[] files = dir.findFiles(s);
        assertNotNull(files);

        // construct set of relative names
        Set<String> names = new HashSet<String>();
        for(FileObject file : files)
            names.add(dir.getName().getRelativeName(file.getName()));

        assertTrue(names.remove("test-data/test-hash-#test.txt"));
        assertTrue(names.isEmpty());

        g = new FileNameGlobbing("test-data/test-hash*");

        // get a selector for this pattern
        s = g.getSelector();
        assertNotNull(s);

        // use the selector to filter
        dir.refresh();
        files = dir.findFiles(s);
        assertNotNull(files);

        // construct set of relative names
        names = new HashSet<String>();
        for(FileObject file : files)
            names.add(dir.getName().getRelativeName(file.getName()));

        assertTrue(names.remove("test-data/test-hash-#test.txt"));
        assertTrue(names.isEmpty());
    }


    @Test
    public void testGetSelectorAllWildAtStart() throws FileSystemException
    {
        FileObject dir = getTestDir();
        FileNameGlobbing g = new FileNameGlobbing("**/file1.txt");

        // get a selector for this pattern
        FileSelector s = g.getSelector();
        assertNotNull(s);

        // use the selector to filter
        dir.refresh();
        FileObject[] files = dir.findFiles(s);
        assertNotNull(files);

        // construct set of relative names
        Set<String> names = new HashSet<String>();
        for(FileObject file : files)
            names.add(dir.getName().getRelativeName(file.getName()));

        assertTrue(names.remove("test-data/read-tests/dir1/subdir2/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir1/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir3/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/file1.txt"));
        assertTrue(names.isEmpty());
    }

    @Test
    public void testGetSelectorAllWildAtSecond() throws FileSystemException
    {
        FileObject dir = getTestDir();
        FileNameGlobbing g = new FileNameGlobbing("test-data/**/file1.txt");

        // get a selector for this pattern
        FileSelector s = g.getSelector();
        assertNotNull(s);

        // use the selector to filter
        dir.refresh();
        FileObject[] files = dir.findFiles(s);
        assertNotNull(files);

        // construct set of relative names
        Set<String> names = new HashSet<String>();
        for(FileObject file : files)
            names.add(dir.getName().getRelativeName(file.getName()));

        assertTrue(names.remove("test-data/read-tests/dir1/subdir2/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir1/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir3/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/file1.txt"));
        assertTrue(names.isEmpty());
    }

    @Test
    public void testGetSelectorWildAndAllWild() throws FileSystemException
    {
        FileObject dir = getTestDir();
        FileNameGlobbing g = new FileNameGlobbing("*/**/file1.txt");

        // get a selector for this pattern
        FileSelector s = g.getSelector();
        assertNotNull(s);

        // use the selector to filter
        dir.refresh();
        FileObject[] files = dir.findFiles(s);
        assertNotNull(files);

        // construct set of relative names
        Set<String> names = new HashSet<String>();
        for(FileObject file : files)
            names.add(dir.getName().getRelativeName(file.getName()));

        assertTrue(names.remove("test-data/read-tests/dir1/subdir2/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir1/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir3/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/file1.txt"));
        assertTrue(names.isEmpty());
    }

    @Test
    public void testGetSelectorOnlyFiles1() throws FileSystemException
    {
        FileObject dir = getTestDir();
        FileNameGlobbing g = new FileNameGlobbing("*/read-tests/dir1/**");

        // get a selector for this pattern
        FileSelector s = g.getSelector();
        assertNotNull(s);

        // use the selector to filter
        dir.refresh();
        FileObject[] files = dir.findFiles(s);
        assertNotNull(files);

        // construct set of relative names
        Set<String> names = new HashSet<String>();
        for(FileObject file : files)
            names.add(dir.getName().getRelativeName(file.getName()));

        assertTrue(names.remove("test-data/read-tests/dir1/subdir1/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir2/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir3/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir1/file2.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir2/file2.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir3/file2.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir1/file3.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir2/file3.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir3/file3.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/file2.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/file3.txt"));

        assertTrue(names.isEmpty());
    }

    @Test
    public void testGetSelectorOnlyFiles2() throws FileSystemException
    {
        FileObject dir = getTestDir();
        FileNameGlobbing g = new FileNameGlobbing("test-data/read-tests/dir1/**");

        // get a selector for this pattern
        FileSelector s = g.getSelector();
        assertNotNull(s);

        // use the selector to filter
        dir.refresh();
        FileObject[] files = dir.findFiles(s);
        assertNotNull(files);

        // construct set of relative names
        Set<String> names = new HashSet<String>();
        for(FileObject file : files)
            names.add(dir.getName().getRelativeName(file.getName()));

        assertTrue(names.remove("test-data/read-tests/dir1/subdir1/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir2/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir3/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir1/file2.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir2/file2.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir3/file2.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir1/file3.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir2/file3.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/subdir3/file3.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/file1.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/file2.txt"));
        assertTrue(names.remove("test-data/read-tests/dir1/file3.txt"));

        assertTrue(names.isEmpty());
    }



    @Test @Ignore
    public void testGetSelectorDirectory() throws FileSystemException
    {
        FileObject dir = getTestDir();
        FileNameGlobbing g = new FileNameGlobbing("*/");

        // get a selector for this pattern
        FileSelector s = g.getSelector();
        assertNotNull(s);

        // use the selector to filter
        dir.refresh();
        FileObject[] files = dir.findFiles(s);
        assertNotNull(files);

        // construct set of relative names
        Set<String> names = new HashSet<String>();
        for(FileObject file : files)
            names.add(dir.getName().getRelativeName(file.getName()));

        assertTrue(names.remove("test-data"));
        assertTrue(names.remove("db"));
        assertFalse("TODO: fix self dir match", names.remove("."));

        assertTrue(names.isEmpty());
    }



    private FileObject getTestDir() throws FileSystemException
    {
        DefaultFileSystemManager manager = new DefaultFileSystemManager();
        manager.addProvider("file", new DefaultLocalFileProvider());
        manager.setCacheStrategy(CacheStrategy.MANUAL);
        manager.setFilesCache(new DefaultFilesCache());
        FileObject file = manager.resolveFile(new File("."), "../src/test/");
        assertTrue("Test directory must exist", file.getType().hasChildren());
        return file;
    }

    private void assertBase(String expected, String pattern)
    {
        FileNameGlobbing g = new FileNameGlobbing(pattern);
        assertEquals("Testing getBase(\"" + pattern + "\")", expected, g.getBase());
    }

    // TODO: "**" "a**a"  "a**/" "**b/" "\c" "?" ("000/*/*"vs"???/*/*")
}



