package com.seeburger.vfs2.provider.jdbctable.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.vfs2.CacheStrategy;
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
    public void testDeleteMissingFile() throws IOException, SQLException
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
        PreparedStatement ps = c.prepareStatement("DELETE FROM tBlobs WHERE cID=?");
        ps.setString(1, "missingfile_" + now);
        int count = ps.executeUpdate();
        assertEquals(1, count); // proof the file existed
        c.commit();
        c.close();

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

        testFile.moveTo(targetFile);

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
    public void testDeleteVanishedFile() throws IOException
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

        // validate content
        InputStream is = testFile.getContent().getInputStream();
        if (is.read() != 2 || is.read() != 3)
            fail("Read wrong content");
        is.close();

        testFile.getContent().close();
    }

}



