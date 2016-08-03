/*
 * SimpleNamedH2Test.java
 *
 * created at 2013-09-25 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable.test;


import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.googlecode.flyway.core.Flyway;
import com.seeburger.vfs2.provider.jdbctable.JdbcDialect;
import com.seeburger.vfs2.provider.jdbctable.JdbcDialectBase;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableFileSystemConfigBuilder;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;


public class SimpleNamedH2Test
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
    public void testTableDefault() throws FileSystemException, SQLException
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
        PreparedStatement ps = dialect.prepareQuery(c, "SELECT cName,cParent,'{table}' FROM {table} WHERE cParent=? AND cName=?");
        ps.setString(1, "/key");
        ps.setString(2, "dir_"+now);

        ResultSet rs = ps.executeQuery();
        assertTrue(rs.next());
        final String name = rs.getString(1);
        final String folder = rs.getString(2);

        // verify the replacement by dialect to be the default table
        final String replacedTable = rs.getString(3);
        assertEquals("tBlobs", replacedTable);

        rs.close(); ps.close(); c.close(); dialect.returnConnection(c);

        assertEquals("dir_"+now, name);
        assertEquals("/key", folder);

        verifyDatabase();
    }

    @Test
    public void testTable2() throws FileSystemException, SQLException
    {
        final long now = System.currentTimeMillis();

        FileSystemOptions opts = new FileSystemOptions();
        JdbcTableFileSystemConfigBuilder builder = JdbcTableFileSystemConfigBuilder.getInstance();
        builder.setTablename(opts, "tBlobs2");

        final FileObject testFile = manager.resolveFile("seejt:///key/dir_"+now, opts);
        assertFalse(testFile.exists());
        assertEquals(FileType.IMAGINARY, testFile.getType());

        testFile.createFolder();
        assertTrue(testFile.exists());
        assertEquals(FileType.FOLDER, testFile.getType());

        verifyDatabase();

        JdbcDialect dialect2 = dialect.cloneDialect("tBlobs2");
        Connection c = dialect2.getConnection();
        PreparedStatement ps = dialect2.prepareQuery(c, "SELECT cName,cParent,'{table}' FROM {table} WHERE cParent=? AND cName=?");
        ps.setString(1, "/key");
        ps.setString(2, "dir_"+now);

        ResultSet rs = ps.executeQuery();
        assertTrue(rs.next());
        final String name = rs.getString(1);
        final String folder = rs.getString(2);

        // verify the replacement by dialect to be the table 2
        final String replacedTable = rs.getString(3);
        assertEquals("tBlobs2", replacedTable);

        rs.close(); ps.close(); c.close(); dialect2.returnConnection(c);

        assertEquals("dir_"+now, name);
        assertEquals("/key", folder);

        verifyDatabase();
    }

    @Test(expected=FileSystemException.class)
    public void testTableWrong() throws FileSystemException, SQLException
    {
        final long now = System.currentTimeMillis();

        FileSystemOptions opts = new FileSystemOptions();
        JdbcTableFileSystemConfigBuilder builder = JdbcTableFileSystemConfigBuilder.getInstance();
        builder.setTablename(opts, "tBlobsMissing");

        final FileObject testFile = manager.resolveFile("seejt:///key/dir_"+now, opts);
        assertFalse(testFile.exists()); // will fail
    }


    static boolean skip = false;

    @BeforeClass
    public static void setupDatabase() throws SQLException
    {
        System.out.println("Starting H2 database");
        JdbcDataSource ds = new JdbcDataSource();
        ds.setUser("VFSTEST");
        ds.setPassword("secret");
        // make sure mem database is not anonymous and will not get droped on conenction close
        ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;JMX=TRUE");

        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        flyway.setLocations("db/migration/h2_derby");
        flyway.setValidateOnMigrate(true);
        flyway.setCleanOnValidationError(true);
        flyway.migrate();

        Connection c = ds.getConnection();
        DatabaseMetaData md = c.getMetaData();
        ResultSet rs = md.getTables(null, null, null, new String[] { "TABLE" });
        while(rs.next())
        {
            System.out.println(rs.getString(1)+"|"+rs.getString(2)+"|"+rs.getString(3)+"|"+rs.getString(4));
        }
        rs.close(); c.close();

        SimpleNamedH2Test.dialect = new JdbcDialectBase(ds);
        SimpleNamedH2Test.dataSource = null;
    }

    void verifyDatabase()
    {
        if (skip)
            return;

        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            c = dialect.getConnection();
            ps = c.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.SESSIONS");
            rs = ps.executeQuery();
            int count = 0;
            while(rs.next())
            {
                count++;
                //System.out.println(" " + count + "  " + rs.getString(1)+"|"+rs.getString(2)+"|"+rs.getString(3)+"|"+rs.getString(4));
            }

            if (count > 1)
            {
                skip = true;
                fail("After this test there are " + count + " sessions active");
            }
        }
        catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            safeClose(rs, ps, c);
        }

    }

    private void safeClose(ResultSet rs, PreparedStatement ps, Connection c)
    {
        try { rs.close(); } catch (Exception ignored) { /* ignored */ }
        try { ps.close(); } catch (Exception ignored) { /* ignored */ }
        try { c.close(); } catch (Exception ignored) { /* ignored */ }
        dialect.returnConnection(c);
    }
}



