package com.seeburger.vfs2.provider.jdbctable.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.derby.jdbc.EmbeddedDataSource40;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.googlecode.flyway.core.Flyway;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;


public class SimpleDerbyTest
{
    private static DataSource dataSource;

    private DefaultFileSystemManager manager;

    @Before
    public void setUp() throws FileSystemException
    {
        manager = new DefaultFileSystemManager();
        manager.addProvider("seejt", new JdbcTableProvider(dataSource));
        manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
        manager.init();
    }

    @After
    public void tearDown()
    {
        manager.close();
    }

    @BeforeClass
    public static void setupDatabase()
    {
        System.out.println("Starting database");
        EmbeddedDataSource40 ds = new EmbeddedDataSource40();
        ds.setUser("SEEASOWN");
        ds.setPassword("secret");
        ds.setCreateDatabase("create");
        ds.setDatabaseName("target/SimpleDerbyTestDB");

        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        flyway.setCleanOnValidationError(true);
        flyway.migrate();

        dataSource = ds;
    }

    @AfterClass
    public static void destroyDatabase()
    {
        System.out.println("Tearing down DB");
        EmbeddedDataSource40 ds = (EmbeddedDataSource40)dataSource; dataSource = null;
        ds.setShutdownDatabase("true");
    }


    @Test
    public void testCreateManager() throws FileSystemException
    {
        final FileObject key = manager.resolveFile("seejt:/key");
        final FileObject root = key.getFileSystem().getRoot();

        assertEquals("/",root.getName().getPath());
        assertEquals("seejt", root.getName().getScheme());
        assertNull(root.getParent());
        System.out.println("root = " + root + " parent=" + root.getParent());
        assertEquals("/key", key.getName().getPath());
    }

    @Test
    public void testReadWrite() throws IOException
    {
        final long now = System.currentTimeMillis();
        final FileObject testFile = manager.resolveFile("seejt:///key/testfile_"+now);
        assertEquals(false, testFile.getContent().isOpen());
        OutputStream os = testFile.getContent().getOutputStream();
        assertEquals(false, testFile.exists());
        assertEquals(true, testFile.getContent().isOpen());
        os.write(1); os.write(2); os.write(3);
        assertEquals(false, testFile.exists());
        os.close();
        assertEquals(true, testFile.exists());
        assertEquals(false, testFile.getContent().isOpen());
        InputStream is = testFile.getContent().getInputStream();
        assertEquals(true, testFile.getContent().isOpen());
        if (is.read() != 1 || is.read() != 2 || is.read() != 3)
            fail("Read wrong content");
        is.close();
        assertEquals(false, testFile.getContent().isOpen());
        testFile.getContent().close();
    }

    @Test
    public void testCreateFolder() throws FileSystemException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/dir_"+now);
        assertEquals(false,  testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFolder();
        assertEquals(true,  testFile.exists());
        assertEquals(FileType.FOLDER, testFile.getType());

        Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT cName,cParent FROM tBlobs WHERE cParent=? AND cName=?");
        ps.setString(1, "/key");
        ps.setString(2, "dir_"+now);
        ResultSet rs = ps.executeQuery();
        assertTrue(rs.next());
        final String name = rs.getString(1);
        assertEquals("dir_"+now, name);
        final String folder = rs.getString(2);
        assertEquals("/key", folder);
        rs.close(); ps.close(); c.close();
    }

    @Test
    public void testCreateFolders() throws FileSystemException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile1 = manager.resolveFile("seejt:///key/dir_"+now+"/sub/file1");
        final FileObject testFile2 = manager.resolveFile("seejt:///key/dir_"+now+"/sub/file2");
        assertEquals(false, testFile1.exists());
        assertEquals(false, testFile2.exists());
        assertEquals(FileType.IMAGINARY, testFile1.getType());
        assertEquals(FileType.IMAGINARY, testFile2.getType());

        final FileObject testFolder = manager.resolveFile("seejt:///key/dir_"+now+"/sub");
        assertEquals(false, testFolder.exists());
        assertEquals(FileType.IMAGINARY, testFolder.getType());

        // creating the files...
        testFile1.createFile();
        testFile2.createFile();
        assertEquals(true, testFile1.exists());
        assertEquals(true, testFile2.exists());
        assertEquals(FileType.FILE, testFile1.getType());
        assertEquals(FileType.FILE, testFile2.getType());

        // .. will also create the parent folder
        assertEquals(true, testFolder.exists());
        assertEquals(FileType.FOLDER, testFolder.getType());

        // ensure we have 2 files in DB ...
        Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT cName FROM tBlobs WHERE cParent=? AND cSize=0");
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
        ps = c.prepareStatement("SELECT cName FROM tBlobs WHERE cParent=? AND cSize=-2");
        ps.setString(1, "/key/dir_"+now);
        rs = ps.executeQuery();
        assertTrue(rs.next());
        name = rs.getString(1);
        assertEquals("sub", name);
        assertFalse(rs.next());
        rs.close(); ps.close();
        c.close();
    }


    @Test
    public void testEmptydirs() throws FileSystemException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/dir2_"+now+"/sub/dir/");
        assertEquals(false,  testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFolder();
        assertEquals(true,  testFile.exists());
        assertEquals(FileType.FOLDER, testFile.getType());
        FileObject parent = testFile.getParent();
        assertEquals(true,  parent.exists());
        assertEquals(FileType.FOLDER, parent.getType());
        assertEquals("sub", parent.getName().getBaseName());
    }

    @Test
    public void testAppearingFolder() throws IOException, SQLException
    {
        final long now = System.currentTimeMillis();
        // now VFS thinks the file exists, we delete it in SQL
        Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("INSERT INTO tBlobs (cParent,cName,cSize,cLastModified,cMarkGarbage) VALUES(?,?,-2,?,?)");
        ps.setString(1, "/key");
        ps.setString(2, "newdir_"+now);
        ps.setLong(3, now);
        ps.setLong(4, now);
        int count = ps.executeUpdate();
        assertEquals(1, count); // proof the file existed
        commitAndClose(ps,c);

        final FileObject testFile = manager.resolveFile("seejt:///key/newdir_"+now);
        assertEquals(true,  testFile.exists());
        assertEquals(FileType.FOLDER, testFile.getType());
    }

    @Test
    public void testCreateRead() throws IOException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/createfile_"+now);
        assertEquals(false,  testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFile();
        assertEquals(true,  testFile.exists());
        assertEquals(FileType.FILE, testFile.getType());

        InputStream is = testFile.getContent().getInputStream();
        assertNotNull(is);

        assertEquals(-1, is.read());

        is.close();
    }

    @Test
    public void testCreateDeleteFile() throws IOException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/createfile_"+now);
        assertEquals(false,  testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFile();
        assertEquals(true,  testFile.exists());
        assertEquals(FileType.FILE, testFile.getType());

        testFile.delete();
        assertEquals(false,  testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());
    }

    @Test
    public void testDeleteVanishedFile() throws IOException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/missingfile_"+now);
        assertEquals(false,  testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFile();
        assertEquals(true,  testFile.exists());
        assertEquals(FileType.FILE, testFile.getType());

        // now VFS thinks the file exists, we delete it in SQL
        Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("DELETE FROM tBlobs WHERE cParent='/key' AND cName=?");
        ps.setString(1, "missingfile_" + now);
        int count = ps.executeUpdate();
        assertEquals(1, count); // proof the file existed
        commitAndClose(ps, c);

        testFile.delete();
        assertEquals(false,  testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());
    }

    @Test
    public void testRename() throws IOException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/renameme_"+now);
        final FileObject targetFile = manager.resolveFile("seejt:///key/renamed_"+now);
        assertEquals(false, testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFile();
        assertEquals(true,  testFile.exists());
        assertEquals(FileType.FILE, testFile.getType());

        testFile.moveTo(targetFile); // TODO: make overwrites this atomic in the provider

        assertEquals(false,  testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        assertEquals(true,  targetFile.exists());
        assertEquals(FileType.FILE, targetFile.getType());
    }

    @Test @Ignore // does not reject rename of open/missing files
    public void testRenameOpen() throws IOException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/renameme_"+now);
        final FileObject targetFile = manager.resolveFile("seejt:///key/renamed_"+now);
        assertEquals(false, testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        OutputStream os = testFile.getContent().getOutputStream();
        assertEquals(false, testFile.exists());
        os.write(255); //os.close();
        assertEquals(true, testFile.exists());
        assertEquals(true,  testFile.isContentOpen());

        testFile.moveTo(targetFile);

        assertEquals(false,  testFile.isContentOpen());
        assertEquals(false,  targetFile.isContentOpen());
    }

    @Test
    public void testRenameContent() throws IOException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/renameme_"+now);
        final FileObject targetFile = manager.resolveFile("seejt:///key/renamed_"+now);
        assertEquals(false, testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        OutputStream os = testFile.getContent().getOutputStream();
        os.write(255); os.close();
        assertEquals(false,  testFile.isContentOpen());
        testFile.moveTo(targetFile);

        InputStream is = targetFile.getContent().getInputStream();
        assertEquals(255,  is.read());
        assertEquals(1, targetFile.getContent().getSize());
        is.close();
    }


    @Test
    public void testDeleteMissingFile() throws IOException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/vanishedgfile_"+now);
        assertEquals(false,  testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.delete();
        assertEquals(false,  testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());
    }


    @Test
    public void testReadVanishedFile() throws IOException, SQLException
    {
        final long now = System.currentTimeMillis();

        final FileObject testFile = manager.resolveFile("seejt:///key/readfile_"+now);
        final FileContent content = testFile.getContent();
        testFile.createFile();
        assertEquals(true, testFile.exists());

        // now VFS thinks the file exists, we delete it in SQL
        Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("DELETE FROM tBlobs WHERE cParent='/key' and cName=?");
        ps.setString(1, "readfile_" + now);
        int count = ps.executeUpdate();
        assertEquals(1, count); // proof the file existed
        c.commit();
        c.close();

        try
        {
            content.getInputStream();
            fail("Expected a FileSystemException");
        }
        catch (FileSystemException fse)
        {
            assertTrue(fse.getCause().toString().contains("row not found"));
        }
    }


    @Test @Ignore
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

        assertEquals(true, testFile.exists());
        assertEquals(false, testFile.getContent().isOpen());
        assertEquals(3,  testFile.getContent().getSize());

        // validate content
        InputStream is = testFile.getContent().getInputStream();
        if (is.read() != 1 || is.read() != 2 || is.read() != 3)
            fail("Read wrong content");
        is.close();

        testFile.getContent().close();
    }

    @Test @Ignore
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

        // validate content
        InputStream is = testFile.getContent().getInputStream();
        if (is.read() != 2 || is.read() != 3)
            fail("Read wrong content");
        is.close();

        testFile.getContent().close();
    }

    private void commitAndClose(PreparedStatement ps, Connection c) throws SQLException
    {
        ps.close();
        c.commit();
        c.close();
    }
}



