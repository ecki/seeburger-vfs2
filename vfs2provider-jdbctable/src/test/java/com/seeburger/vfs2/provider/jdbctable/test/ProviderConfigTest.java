package com.seeburger.vfs2.provider.jdbctable.test;


import static org.junit.Assert.*;

import javax.sql.DataSource;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.seeburger.vfs2.provider.jdbctable.JdbcDialect;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableFileSystemConfigBuilder;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;


public class ProviderConfigTest
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
    public void testCompareSameInstancesDefault() throws FileSystemException
    {
        FileSystemOptions opts1 = new FileSystemOptions();
        FileObject key1 = manager.resolveFile("seejt:/key", opts1);
        FileSystem fs1 = key1.getFileSystem();

        FileSystemOptions opts2 = new FileSystemOptions();
        FileObject key2 = manager.resolveFile("seejt:/key", opts2);
        FileSystem fs2 = key2.getFileSystem();

        assertSame(fs1, fs2);
    }

    @Test
    public void testCompareSameInstancesWrite() throws FileSystemException
    {
        FileSystemOptions opts1 = new FileSystemOptions();
        JdbcTableFileSystemConfigBuilder builder = JdbcTableFileSystemConfigBuilder.getInstance();
        builder.setWriteMode(opts1, true);
        FileObject key1 = manager.resolveFile("seejt:/key", opts1);
        FileSystem fs1 = key1.getFileSystem();

        FileSystemOptions opts2 = new FileSystemOptions();
        builder.setWriteMode(opts2, true);
        FileObject key2 = manager.resolveFile("seejt:/key", opts2);
        FileSystem fs2 = key2.getFileSystem();

        assertSame(fs1, fs2);
    }

    @Test
    public void testCompareSameInstancesNoWrite() throws FileSystemException
    {
        FileSystemOptions opts1 = new FileSystemOptions();
        JdbcTableFileSystemConfigBuilder builder = JdbcTableFileSystemConfigBuilder.getInstance();
        builder.setWriteMode(opts1, false);
        FileObject key1 = manager.resolveFile("seejt:/key", opts1);
        FileSystem fs1 = key1.getFileSystem();

        FileSystemOptions opts2 = new FileSystemOptions();
        builder.setWriteMode(opts2, false);
        FileObject key2 = manager.resolveFile("seejt:/key", opts2);
        FileSystem fs2 = key2.getFileSystem();

        assertSame(fs1, fs2);
    }

    @Test
    public void testCompareSameInstancesTables() throws FileSystemException
    {
        FileSystemOptions opts1 = new FileSystemOptions();
        JdbcTableFileSystemConfigBuilder builder = JdbcTableFileSystemConfigBuilder.getInstance();
        builder.setTablename(opts1, "t1");
        FileObject key1 = manager.resolveFile("seejt:/key", opts1);
        FileSystem fs1 = key1.getFileSystem();

        FileSystemOptions opts2 = new FileSystemOptions();
        builder.setTablename(opts2, new String("t1")); // different string instance
        FileObject key2 = manager.resolveFile("seejt:/key", opts2);
        FileSystem fs2 = key2.getFileSystem();

        assertSame(fs1, fs2);
    }

    @Test
    public void testCompareDifferentWrite() throws FileSystemException
    {
        FileSystemOptions opts1 = new FileSystemOptions();
        JdbcTableFileSystemConfigBuilder builder = JdbcTableFileSystemConfigBuilder.getInstance();
        builder.setWriteMode(opts1, true);
        FileObject key1 = manager.resolveFile("seejt:/key", opts1);
        FileSystem fs1 = key1.getFileSystem();

        FileSystemOptions opts2 = new FileSystemOptions();
        builder.setWriteMode(opts2, false); // different string instance
        FileObject key2 = manager.resolveFile("seejt:/key", opts2);
        FileSystem fs2 = key2.getFileSystem();

        assertNotSame(fs1, fs2);
    }

    @Test
    public void testCompareDifferentTable() throws FileSystemException
    {
        FileSystemOptions opts1 = new FileSystemOptions();
        JdbcTableFileSystemConfigBuilder builder = JdbcTableFileSystemConfigBuilder.getInstance();
        builder.setTablename(opts1, "t1");
        FileObject key1 = manager.resolveFile("seejt:/key", opts1);
        FileSystem fs1 = key1.getFileSystem();

        FileSystemOptions opts2 = new FileSystemOptions();
        builder.setTablename(opts2, "t2");
        FileObject key2 = manager.resolveFile("seejt:/key", opts2);
        FileSystem fs2 = key2.getFileSystem();

        assertNotSame(fs1, fs2);
    }

    @Test
    public void testCompareSameInstancesNoopt() throws FileSystemException
    {
        FileSystemOptions opts = new FileSystemOptions();
        FileObject key1 = manager.resolveFile("seejt:/key", opts);
        FileSystem fs1 = key1.getFileSystem();

        FileObject key2 = manager.resolveFile("seejt:/key");
        FileSystem fs2 = key2.getFileSystem();

        assertSame(fs1, fs2);
    }
}



