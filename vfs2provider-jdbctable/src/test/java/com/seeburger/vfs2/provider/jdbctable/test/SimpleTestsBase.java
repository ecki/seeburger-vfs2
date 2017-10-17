/*
 * SimpleTestBase.java
 *
 * created at 2013-09-07 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable.test;


import static org.junit.Assert.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.seeburger.vfs2.operations.BulkSetAttributeOperation;
import com.seeburger.vfs2.operations.ExpireFilesOperation;
import com.seeburger.vfs2.provider.jdbctable.JdbcDialect;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableOperationProvider;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;


public abstract class SimpleTestsBase
{
    static DataSource dataSource;
    static JdbcDialect dialect;

    DefaultFileSystemManager manager;

    @Before
    public void setUp() throws FileSystemException
    {
        manager = new DefaultFileSystemManager();
        if (dialect != null)
            manager.addProvider("seejt", new JdbcTableProvider(dialect));
        else
            manager.addProvider("seejt", new JdbcTableProvider(dataSource));
        manager.addOperationProvider("seejt", JdbcTableOperationProvider.getInstance());
        manager.addProvider("file", new DefaultLocalFileProvider());
        manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
        manager.init();
    }

    @After
    public void tearDown()
    {
        manager.close();
    }


    @Test
    public void testCreateManager() throws FileSystemException
    {
        final FileObject key = manager.resolveFile("seejt:/key");
        final FileObject root = key.getFileSystem().getRoot();

        assertEquals("/",root.getName().getPath());
        assertEquals("seejt", root.getName().getScheme());
        assertNull(root.getParent());

        assertEquals("/key", key.getName().getPath());

        assertEquals(root, key.getParent());

        verifyDatabase();
    }

    @Test
    public void testReadWrite() throws IOException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/testfile_"+now);

        assertFalse(testFile.getContent().isOpen());

        OutputStream os = testFile.getContent().getOutputStream();
        assertFalse(testFile.exists());
        assertTrue(testFile.getContent().isOpen());

        os.write(1); os.write(2); os.write(3);
        assertFalse(testFile.exists());

        os.close();
        assertTrue(testFile.exists());
        assertFalse(testFile.getContent().isOpen());

        // validate content
        InputStream is = testFile.getContent().getInputStream();
        assertTrue(testFile.getContent().isOpen());
        DataInputStream dis = new DataInputStream(is);
        byte[] buf = new byte[3];
        dis.readFully(buf);
        dis.close();
        testFile.getContent().close();
        byte[] expectedBuf = new byte[3]; expectedBuf[0]=1; expectedBuf[1]=2; expectedBuf[2]=3;
        assertArrayEquals(expectedBuf, buf);
        assertFalse(testFile.getContent().isOpen());

        verifyDatabase();
    }

    @Test
    public void testCreateFolder() throws FileSystemException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/dir_"+now);
        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFolder();
        assertTrue(testFile.exists());
        assertEquals(FileType.FOLDER, testFile.getType());

        verifyDatabase();

        Connection c = dialect.getConnection();
        PreparedStatement ps = dialect.prepareQuery(c, "SELECT cName,cParent FROM {table} WHERE cParent=? AND cName=?");
        ps.setString(1, "/key");
        ps.setString(2, "dir_"+now);

        ResultSet rs = ps.executeQuery();
        assertTrue(rs.next());
        final String name = rs.getString(1);
        final String folder = rs.getString(2);
        rs.close(); ps.close(); c.close(); dialect.returnConnection(c);

        assertEquals("dir_"+now, name);
        assertEquals("/key", folder);

        verifyDatabase();
    }

    @Test
    public void testCreateFolders() throws FileSystemException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile1 = manager.resolveFile("seejt:///key/dir_"+now+"/sub/file1");
        final FileObject testFile2 = manager.resolveFile("seejt:///key/dir_"+now+"/sub/file2");
        assertFalse(testFile1.exists());
        assertFalse(testFile2.exists());
        assertEquals(FileType.IMAGINARY, testFile1.getType());
        assertEquals(FileType.IMAGINARY, testFile2.getType());

        final FileObject testFolder = manager.resolveFile("seejt:///key/dir_"+now+"/sub");
        assertFalse(testFolder.exists());
        assertEquals(FileType.IMAGINARY, testFolder.getType());

        // creating the files...
        testFile1.createFile();
        testFile2.createFile();
        assertTrue(testFile1.exists());
        assertTrue(testFile2.exists());
        assertEquals(FileType.FILE, testFile1.getType());
        assertEquals(FileType.FILE, testFile2.getType());

        // .. will also create the parent folder
        assertTrue(testFolder.exists());
        assertEquals(FileType.FOLDER, testFolder.getType());

        // ensure we have 2 files in DB ...
        Connection c = dialect.getConnection();
        PreparedStatement ps = dialect.prepareQuery(c, "SELECT cName FROM {table} WHERE cParent=? AND cSize=0");
        ps.setString(1, "/key/dir_"+now+"/sub");
        ResultSet rs = ps.executeQuery();
        assertTrue(rs.next());
        String name = rs.getString(1);
        int flag = 0;
        if (name.equals("file1"))
            flag |= 1;
        else if (name.equals("file2"))
            flag |= 2;
        assertTrue(rs.next());
        name = rs.getString(1);
        if (name.equals("file1"))
            flag |= 1;
        else if (name.equals("file2"))
            flag |= 2;
        assertEquals(3, flag);
        assertFalse(rs.next());
        rs.close(); ps.close();

        // ...and one auto-created folder
        ps = dialect.prepareQuery(c, "SELECT cName FROM {table} WHERE cParent=? AND cSize=-2");
        ps.setString(1, "/key/dir_"+now);
        rs = ps.executeQuery();
        assertTrue(rs.next());
        name = rs.getString(1);
        assertEquals("sub", name);
        assertFalse(rs.next());
        rs.close(); ps.close();
        c.close(); dialect.returnConnection(c);

        verifyDatabase();
    }


    @Test
    public void testCreateAndListFolders() throws FileSystemException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile1 = manager.resolveFile("seejt:///key/dir_"+now+"/sub/file1");
        final FileObject testFile2 = manager.resolveFile("seejt:///key/dir_"+now+"/sub/file2");
        assertFalse(testFile1.exists());
        assertFalse(testFile2.exists());
        assertEquals(FileType.IMAGINARY, testFile1.getType());
        assertEquals(FileType.IMAGINARY, testFile2.getType());

        final FileObject testFolder = manager.resolveFile("seejt:///key/dir_"+now+"/sub");
        assertFalse(testFolder.exists());
        assertEquals(FileType.IMAGINARY, testFolder.getType());

        // creating the files...
        testFile1.createFile();
        testFile2.createFile();
        assertTrue(testFile1.exists());
        assertTrue(testFile2.exists());
        assertEquals(FileType.FILE, testFile1.getType());
        assertEquals(FileType.FILE, testFile2.getType());

        // .. will also create the parent folder
        assertTrue(testFolder.exists());
        assertEquals(FileType.FOLDER, testFolder.getType());

        // ensure we have 2 subfolders to list
        FileObject[] children = testFolder.getChildren();
        assertEquals(2, children.length);
        for(int i=0;i<children.length;i++)
        {
            //System.out.println("f=" + children[i]);
            assertTrue(children[i].exists());
            assertEquals(FileType.FILE, children[i].getType());
            assertEquals(0, children[i].getContent().getSize());
        }

        verifyDatabase();
    }



    @Test
    public void testEmptydirs() throws FileSystemException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/dir2_"+now+"/sub/dir/");
        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFolder();
        assertTrue(testFile.exists());
        assertEquals(FileType.FOLDER, testFile.getType());
        FileObject parent = testFile.getParent();
        assertTrue(parent.exists());
        assertEquals(FileType.FOLDER, parent.getType());
        assertEquals("sub", parent.getName().getBaseName());

        verifyDatabase();
    }

    @Test
    public void testAppearingFolder() throws IOException, SQLException
    {
        final long now = System.currentTimeMillis();
        // now VFS thinks the file exists, we delete it in SQL
        Connection c = dialect.getConnection();
        PreparedStatement ps = dialect.prepareQuery(c, "INSERT INTO {table} (cParent,cName,cSize,cLastModified,cMarkGarbage) VALUES(?,?,-2,?,?)");
        ps.setString(1, "/key");
        ps.setString(2, "newdir_"+now);
        ps.setLong(3, now);
        ps.setLong(4, now);
        int count = ps.executeUpdate();
        assertEquals(1, count); // proof the file existed
        commitAndClose(ps,c);

        final FileObject testFile = manager.resolveFile("seejt:///key/newdir_"+now);
        assertTrue(testFile.exists());
        assertEquals(FileType.FOLDER, testFile.getType());

        verifyDatabase();
    }

    @Test
    public void testCreateRead() throws IOException
    {
        final long now = System.currentTimeMillis();
        final FileObject testFile = manager.resolveFile("seejt:///key/createfile_"+now);
        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFile();
        assertTrue(testFile.exists());
        assertEquals(FileType.FILE, testFile.getType());

        InputStream is = testFile.getContent().getInputStream();
        assertNotNull(is);

        assertEquals(-1, is.read());
        is.close();

        verifyDatabase();
    }

    @Test
    public void testCreateDeleteFile() throws IOException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/createfile_"+now);
        verifyDatabase();
        assertFalse(testFile.exists());
        verifyDatabase();
        assertEquals(FileType.IMAGINARY, testFile.getType());

        verifyDatabase();

        testFile.createFile();
        assertTrue(testFile.exists());
        assertEquals(FileType.FILE, testFile.getType());

        verifyDatabase();

        testFile.delete();
        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        verifyDatabase();
    }

    @Test
    public void testDeleteVanishedFile() throws IOException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/missingfile_"+now);
        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFile();
        assertTrue(testFile.exists());
        assertEquals(FileType.FILE, testFile.getType());

        // now VFS thinks the file exists, we delete it in SQL
        Connection c = dialect.getConnection();
        PreparedStatement ps = dialect.prepareQuery(c, "DELETE FROM {table} WHERE cParent='/key' AND cName=?");
        ps.setString(1, "missingfile_" + now);
        int count = ps.executeUpdate();
        assertEquals(1, count); // proof the file existed
        commitAndClose(ps, c);

        testFile.delete();
        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        verifyDatabase();
    }

    @Test(expected=FileSystemException.class)
    public void testRenameNonexistingOverNonexisting() throws FileSystemException
    {
        final long now = System.nanoTime();

        // create one file and make sure the target does not exist
        final FileObject testFile = manager.resolveFile("seejt:///key/testRenameNonexistingOverNonexisting_A_"+now);
        final FileObject targetFile = manager.resolveFile("seejt:///key/testRenameNonexistingOverNonexisting_B_"+now);

        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());
        assertFalse(targetFile.exists());
        assertEquals(FileType.IMAGINARY, targetFile.getType());

        verifyDatabase();

        // rename non-existing file to non-existing file, will fail
        testFile.moveTo(targetFile);

        verifyDatabase();
    }

    @Test(expected=FileSystemException.class)
    public void testRenameNonexistingOverExisting() throws FileSystemException
    {
        final long now = System.nanoTime();

        // create one file and make sure the target does not exist
        final FileObject testFile = manager.resolveFile("seejt:///key/testRenameNonexistingOverExisting_A_"+now);
        final FileObject targetFile = manager.resolveFile("seejt:///key/testRenameNonexistingOverExisting_B_"+now);

        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());
        assertFalse(targetFile.exists());
        assertEquals(FileType.IMAGINARY, targetFile.getType());
        targetFile.createFile();
        assertTrue(targetFile.exists());
        assertEquals(FileType.FILE, targetFile.getType());

        // rename non-existing file over existing, will fail
        testFile.moveTo(targetFile);

        verifyDatabase();
    }

    @Test
    public void testRenameOverNonexisting() throws IOException, SQLException
    {
        final long now = System.nanoTime();

        // create one file and make sure the target does not exist
        final FileObject testFile = manager.resolveFile("seejt:///key/testRenameOverNonexisting_A_"+now);
        final FileObject targetFile = manager.resolveFile("seejt:///key/testRenameOverNonexisting_B_"+now);

        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());
        testFile.createFile();
        assertTrue(testFile.exists());
        assertEquals(FileType.FILE, testFile.getType());

        assertFalse(targetFile.exists());
        assertEquals(FileType.IMAGINARY, targetFile.getType());

        // rename existing file to non-existing
        testFile.moveTo(targetFile);

        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        assertTrue(targetFile.exists());
        assertEquals(FileType.FILE, targetFile.getType());

        verifyDatabase();
    }

    @Test
    public void testRenameOverExisting() throws IOException, SQLException
    {
        final long now = System.nanoTime();

        // create two files
        final FileObject testFile = manager.resolveFile("seejt:///key/testRenameOverExisting_A_"+now);
        final FileObject targetFile = manager.resolveFile("seejt:///key/testRenameOverExisting_B_"+now);

        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFile();
        assertTrue(testFile.exists());
        assertEquals(FileType.FILE, testFile.getType());

        assertEquals(FileType.IMAGINARY, targetFile.getType());
        assertFalse(targetFile.exists());
        targetFile.createFile();

        assertTrue(targetFile.exists());
        assertEquals(FileType.FILE, targetFile.getType());

        // rename existing file over existing target
        testFile.moveTo(targetFile); // TODO: make overwrites this atomic in the provider

        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        assertTrue(targetFile.exists());
        assertEquals(FileType.FILE, targetFile.getType());

        verifyDatabase();
    }


    @Test @Ignore // does not reject rename of open/missing files
    public void testRenameOpen() throws IOException, SQLException
    {
        final long now = System.nanoTime();

        final FileObject testFile = manager.resolveFile("seejt:///key/testRenameOpen_A_"+now);
        final FileObject targetFile = manager.resolveFile("seejt:///key/testRenameOpen_B_"+now);
        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        OutputStream os = testFile.getContent().getOutputStream();
        assertFalse(testFile.exists());
        os.write(255); //os.close();
        assertTrue(testFile.exists());
        assertTrue(testFile.isContentOpen());

        testFile.moveTo(targetFile);

        assertFalse(testFile.isContentOpen());
        assertFalse(targetFile.isContentOpen());

        verifyDatabase();
    }

    @Test
    public void testRenameContent() throws IOException, SQLException
    {
        final long now = System.nanoTime();

        final FileObject testFile = manager.resolveFile("seejt:///key/testRenameContent_A_"+now);
        final FileObject targetFile = manager.resolveFile("seejt:///key/testRenameContent_B_"+now);
        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        OutputStream os = testFile.getContent().getOutputStream();
        os.write(255); os.close();
        assertFalse(testFile.isContentOpen());
        testFile.moveTo(targetFile);

        InputStream is = targetFile.getContent().getInputStream();
        assertEquals(255,  is.read());
        assertEquals(1, targetFile.getContent().getSize());
        is.close();

        verifyDatabase();
    }


    @Test
    public void testDeleteMissingFile() throws IOException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/vanishedgfile_"+now);
        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.delete();
        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        verifyDatabase();
    }


    @Test
    public void testReadVanishedFile() throws IOException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/readfile_"+now);
        final FileContent content = testFile.getContent();
        testFile.createFile();
        assertTrue(testFile.exists());

        // now VFS thinks the file exists, we delete it in SQL
        Connection c = dialect.getConnection();
        PreparedStatement ps = dialect.prepareQuery(c, "DELETE FROM {table} WHERE cParent='/key' and cName=?");
        ps.setString(1, "readfile_" + now);
        int count = ps.executeUpdate();
        assertEquals(1, count); // proof the file existed
        commitAndClose(ps, c);

        try
        {
            content.getInputStream();
            fail("Expected a FileSystemException");
        }
        catch (FileSystemException fse)
        {
            assertTrue(fse.getCause().toString().contains("row not found"));
        }

        verifyDatabase();
    }


    @Test
    public void testAppend() throws IOException
    {
        final long now = System.currentTimeMillis();

        FileObject testFile = manager.resolveFile("seejt:///key/appendfile_"+now);

        OutputStream os = testFile.getContent().getOutputStream();
        os.write(1);
        os.close();

        testFile = manager.resolveFile("seejt:///key/appendfile_"+now);

        os = testFile.getContent().getOutputStream(true);
        os.write(2); os.write(3);
        os.close();

        assertTrue(testFile.exists());
        assertFalse(testFile.getContent().isOpen());
        assertEquals(3,  testFile.getContent().getSize());

        // validate content
        InputStream is = testFile.getContent().getInputStream();
        assertTrue(testFile.getContent().isOpen());
        DataInputStream dis = new DataInputStream(is);
        byte[] buf = new byte[3];
        dis.readFully(buf);
        dis.close();
        assertFalse(testFile.getContent().isOpen());
        byte[] expectedBuf = new byte[3]; expectedBuf[0]=1; expectedBuf[1]=2; expectedBuf[2]=3;
        assertArrayEquals(expectedBuf, buf);

        testFile.getContent().close();

        verifyDatabase();
    }

    @Test
    public void testMultipleWrites() throws IOException
    {
        final long now = System.currentTimeMillis();

        FileObject testFile = manager.resolveFile("seejt:///key/multiwrite_"+now);

        OutputStream os = testFile.getContent().getOutputStream();
        os.write(1);
        byte buf[] = new byte[2]; buf[0] = 2; buf[1] = 3;
        os.write(buf);
        os.write(buf);
        os.flush();
        os.write(buf);
        os.close();

        assertTrue(testFile.exists());
        assertFalse(testFile.getContent().isOpen());
        assertEquals(7,  testFile.getContent().getSize());

        // validate content
        InputStream is = testFile.getContent().getInputStream();
        assertTrue(testFile.getContent().isOpen());
        DataInputStream dis = new DataInputStream(is);
        buf = new byte[7];
        dis.readFully(buf);
        dis.close();
        assertFalse(testFile.getContent().isOpen());
        testFile.getContent().close();

        byte[] expectedBuf = new byte[] { 1, 2, 3, 2, 3, 2, 3};
        assertArrayEquals(expectedBuf, buf);

        verifyDatabase();
    }


    @Test
    public void testOverwrite() throws IOException
    {
        final long now = System.currentTimeMillis();
        FileObject testFile = manager.resolveFile("seejt:///key/appendfile_"+now);

        OutputStream os = testFile.getContent().getOutputStream();
        os.write(1);
        os.close();

        testFile = manager.resolveFile("seejt:///key/appendfile_"+now);

        os = testFile.getContent().getOutputStream(false);
        os.write(2); os.write(3);
        os.close();

        assertEquals(2,  testFile.getContent().getSize());
        assertFalse(testFile.getContent().isOpen());

        // validate content
        InputStream is = testFile.getContent().getInputStream();
        assertTrue(testFile.getContent().isOpen());
        DataInputStream dis = new DataInputStream(is);
        byte[] buf = new byte[2];
        dis.readFully(buf);
        dis.close();
        assertFalse(testFile.getContent().isOpen());
        byte[] expectedBuf = new byte[2]; expectedBuf[0]=2; expectedBuf[1]=3;
        assertArrayEquals(expectedBuf, buf);

        testFile.getContent().close();

        verifyDatabase();
    }

    @Test @Ignore // does not work as directories dont update if children change
    public void testLastModifiedDir() throws FileSystemException
    {
        final long DELAY = 30;
        final long now = System.currentTimeMillis();
        FileObject dir = manager.resolveFile("seejt:///key/lastmodifieddir_"+now);
        //dir.delete(Selectors.SELECT_ALL);

        dir.createFolder();
        dir.refresh();
        long created = dir.getContent().getLastModifiedTime();
        try { Thread.sleep(DELAY); } catch (InterruptedException ignored) { /* ignored */ }

        FileObject file1 = dir.resolveFile("child1");
        file1.createFile();

        dir.refresh();
        long modified1 = dir.getContent().getLastModifiedTime();
        try { Thread.sleep(DELAY); } catch (InterruptedException ignored) { /* ignored */ }

        FileObject file2 = dir.resolveFile("child2");
        file2.createFolder();

        dir.refresh();
        long modified2 = dir.getContent().getLastModifiedTime();
        try { Thread.sleep(DELAY); } catch (InterruptedException ignored) { /* ignored */ }

        FileObject file21 = file2.resolveFile("child21");
        file21.createFolder();

        dir.refresh();
        long modified3 = dir.getContent().getLastModifiedTime();

        assertTrue(created < modified1);
        assertTrue(modified1 < modified2);

        assertEquals(modified2,  modified3);

        verifyDatabase();
    }

    @Test
    public void testMarkTime() throws FileSystemException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile1 = manager.resolveFile("seejt:///key/marktime_"+now);
        assertFalse(testFile1.exists());
        assertEquals(FileType.IMAGINARY, testFile1.getType());
        testFile1.createFile();
        assertTrue(testFile1.exists());
        assertEquals(FileType.FILE, testFile1.getType());

        // ensure markTime is initialized to lastModified if freshly created
        assertEquals(Long.valueOf(testFile1.getContent().getLastModifiedTime()), testFile1.getContent().getAttribute("markTime"));

        // support Long values
        testFile1.getContent().setAttribute("markTime", Long.valueOf(1));
        testFile1.refresh();
        assertEquals(Long.valueOf(1), testFile1.getContent().getAttribute("markTime"));

        // support String values
        testFile1.getContent().setAttribute("markTime", "1");
        testFile1.close(); // TODO: currently required, will cache the string in DefaultFileContent
        assertEquals(Long.valueOf(1), testFile1.getContent().getAttribute("markTime"));

        // support null as alias for current
        testFile1.getContent().setAttribute("markTime", null);
        testFile1.close(); // TODO: currently required, will cache the null in DefaultFileContent
        long current = ((Long)(testFile1.getContent().getAttribute("markTime"))).longValue();
        long diff = current - now;
        assertTrue(diff < 10000);

        // and finally find connection leaks
        verifyDatabase();
    }

    @Test
    public void testMarkTimeMissing() throws FileSystemException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile1 = manager.resolveFile("seejt:///key/marktime_"+now);
        assertFalse(testFile1.exists());

        // now we invoke the markTime and verify the exception
        try
        {
            testFile1.getContent().setAttribute("markTime", Long.valueOf(1));
            fail("Setting attributre on missing file should fail");
        }
        catch(FileSystemException ex)
        {
            assertTrue("Exception must contain attribute name. ex=" +ex, ex.getMessage().contains("markTime"));
            assertTrue("Exception must contain file name. ex="+ex, ex.getMessage().contains("key/marktime_"));
        } finally {
            // and finally find connection leaks
            verifyDatabase();
        }
    }

    @Test
    public void testMarkTimeCorrupt() throws FileSystemException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile1 = manager.resolveFile("seejt:///key/marktime_"+now);
        testFile1.createFile();

        try
        {
            // now we remove it from db but do not refresh
            Connection c = dialect.getConnection();
            PreparedStatement ps = dialect.prepareQuery(c, "DELETE FROM {table} WHERE cParent='/key' AND cName=?");
            ps.setString(1, "marktime_"+now);
            int count = ps.executeUpdate();
            assertEquals(1, count); // proof the file existed
            commitAndClose(ps, c);

            // and finally (without refresh) we do it again
            testFile1.getContent().setAttribute("markTime", Long.valueOf(1));
            fail("Setting attribute on deleted record should fail");
        }
        catch(FileSystemException ex)
        {
            assertTrue("Exception must contain attribute name. ex=" +ex, ex.getMessage().contains("markTime"));
            assertTrue("Exception must contain file name. ex="+ex, ex.getMessage().contains("key/marktime_"));

            Throwable cause = ex.getCause();
            assertEquals(SQLException.class, cause.getClass());
            assertTrue("Exception must contain file name. ex="+cause, cause.getMessage().contains("key/marktime_"));
            assertTrue("Exception must talk about corruption. ex="+cause, cause.getMessage().contains("consistency problem"));
        }
        finally
        {
            // and finally find connection leaks
            verifyDatabase();
        }
    }


    @Test
    public void testBulkSet() throws FileSystemException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile1 = manager.resolveFile("seejt:///key/bulkset_"+now+"/file1");
        assertFalse(testFile1.exists());
        assertEquals(FileType.IMAGINARY, testFile1.getType());
        testFile1.createFile();
        assertTrue(testFile1.exists());
        assertEquals(FileType.FILE, testFile1.getType());
        testFile1.close(); // avoid attribute cache

        final FileObject testFile2 = manager.resolveFile("seejt:///key/bulkset_"+now+"/file2");
        assertFalse(testFile2.exists());
        assertEquals(FileType.IMAGINARY, testFile2.getType());
        testFile2.createFile();
        assertTrue(testFile2.exists());
        assertEquals(FileType.FILE, testFile2.getType());
        testFile2.close(); // avoid attribute cache

        final FileObject testParent = manager.resolveFile("seejt:///key/bulkset_"+now);
        assertTrue(testParent.exists());
        assertEquals(FileType.FOLDER, testParent.getType());

        // make sure all files return operation and none is reused
        BulkSetAttributeOperation op1a = (BulkSetAttributeOperation)testFile1.getFileOperations().getOperation(BulkSetAttributeOperation.class);
        assertNotNull(op1a);
        BulkSetAttributeOperation op2 = (BulkSetAttributeOperation)testFile2.getFileOperations().getOperation(BulkSetAttributeOperation.class);
        assertNotNull(op2);
        assertNotSame(op1a, op2);
        BulkSetAttributeOperation op1b = (BulkSetAttributeOperation)testFile1.getFileOperations().getOperation(BulkSetAttributeOperation.class);
        assertNotNull(op1b);
        assertNotSame(op1a, op1b);

        // now prepare actual bulk operation on parent (for relative name checking)
        BulkSetAttributeOperation op = (BulkSetAttributeOperation)testParent.getFileOperations().getOperation(BulkSetAttributeOperation.class);
        assertNotNull(op);

        op.setAttribute("markTime", String.valueOf(Long.MAX_VALUE));
        // test with one relative and one absolute and one missing relative file name
        List<String> files = Arrays.asList("file1", "/key/bulkset_"+now+"/file2", "file3");
        op.setFilesList(files);

        op.process();

        // now verify the time stamps have actually changed
        FileObject verifyFile = manager.resolveFile("seejt:///key/bulkset_"+now+"/file1");
        long markTime = ((Long)(verifyFile.getContent().getAttribute("markTime"))).longValue();
        assertEquals(Long.MAX_VALUE, markTime);

        verifyFile = manager.resolveFile("seejt:///key/bulkset_"+now+"/file2");
        markTime = ((Long)(verifyFile.getContent().getAttribute("markTime"))).longValue();
        assertEquals(Long.MAX_VALUE, markTime);

        // make sure the missing file has not been created
        verifyFile = manager.resolveFile("seejt:///key/bulkset_"+now+"/file3");
        assertFalse(verifyFile.exists());

        // check that exactly the missing file was reported
        Collection<String> result = op.getMissingFiles();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains("file3"));

        // and finally find connection leaks
        verifyDatabase();
    }

    @Test
    public void testExpire() throws FileSystemException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile1 = manager.resolveFile("seejt:///key/expire_"+now+"/in/file1");
        testFile1.createFile();
        testFile1.getContent().setAttribute("markTime", "1");
        assertTrue(testFile1.exists());
        assertEquals(FileType.FILE, testFile1.getType());
        testFile1.close(); // avoid attribute cache

        final FileObject testSubFile1 = manager.resolveFile("seejt:///key/expire_"+now+"/in/sub/file1");
        testSubFile1.createFile();
        testSubFile1.getContent().setAttribute("markTime", "1");
        assertTrue(testSubFile1.exists());
        assertEquals(FileType.FILE, testSubFile1.getType());
        testSubFile1.close(); // avoid attribute cache

        final FileObject testFile2 = manager.resolveFile("seejt:///key/expire_"+now+"/in/file2");
        testFile2.createFile();
        testFile2.getContent().setAttribute("markTime", "2");
        assertTrue(testFile2.exists());
        assertEquals(FileType.FILE, testFile2.getType());
        testFile2.close(); // avoid attribute cache

        final FileObject testFile3 = manager.resolveFile("seejt:///key/expire_"+now+"/in/file3");
        testFile3.createFile();
        testFile3.getContent().setAttribute("markTime", "3");
        assertTrue(testFile3.exists());
        assertEquals(FileType.FILE, testFile3.getType());
        testFile3.close(); // avoid attribute cache

        final FileObject testFile0 = manager.resolveFile("seejt:///key/expire_"+now+"/inout/file0");
        testFile0.createFile();
        testFile0.getContent().setAttribute("markTime", "0");
        assertTrue(testFile0.exists());
        assertEquals(FileType.FILE, testFile0.getType());
        testFile0.close(); // avoid attribute cache

        // we now have 5 files and check if prefix and cutoff timestamp to expire them works
        ExpireFilesOperation op = (ExpireFilesOperation)testFile0.getFileOperations().getOperation(ExpireFilesOperation.class);

        op.setParentPrefix("/key/expire_"+now+"/in");
        op.setTimeBefore(2l);
        op.process();
        int count = op.getDeleteCount();
        assertEquals(2, count); // only file1 and sub/file1

        FileObject verifyFile = manager.resolveFile("seejt:///key/expire_"+now+"/inout/file0");
        assertTrue(verifyFile.exists());
        verifyFile = manager.resolveFile("seejt:///key/expire_"+now+"/in/file1");
        assertFalse(verifyFile.exists());
        verifyFile = manager.resolveFile("seejt:///key/expire_"+now+"/in/sub/file1");
        assertFalse(verifyFile.exists());
        verifyFile = manager.resolveFile("seejt:///key/expire_"+now+"/in/file2");
        assertTrue(verifyFile.exists());
        verifyFile = manager.resolveFile("seejt:///key/expire_"+now+"/in/file3");
        assertTrue(verifyFile.exists());

        // and finally find connection leaks
        verifyDatabase();
    }


    private void commitAndClose(PreparedStatement ps, Connection c) throws SQLException
    {
        ps.close();
        c.commit();
        c.close();
        dialect.returnConnection(c);
    }

    abstract void verifyDatabase();



}



