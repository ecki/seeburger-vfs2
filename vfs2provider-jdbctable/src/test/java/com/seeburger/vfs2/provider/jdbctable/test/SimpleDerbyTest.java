package com.seeburger.vfs2.provider.jdbctable.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sql.DataSource;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.derby.jdbc.EmbeddedDataSource40;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
        assertEquals(true, testFile.getContent().isOpen());
        os.write(1); os.write(2); os.write(3); os.close();
        assertEquals(false, testFile.getContent().isOpen());
        InputStream is = testFile.getContent().getInputStream();
        assertEquals(true, testFile.getContent().isOpen());
        if (is.read() != 1 || is.read() != 2 || is.read() != 3)
            fail("Read wrong content");
        is.close();
        assertEquals(false, testFile.getContent().isOpen());
        testFile.getContent().close();
    }
}



