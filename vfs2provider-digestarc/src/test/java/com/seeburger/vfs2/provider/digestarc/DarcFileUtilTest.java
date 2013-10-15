/*
 * DarcFileUtilTest.java
 *
 * created at 2013-10-15 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import static org.junit.Assert.*;

import java.io.File;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.cache.DefaultFilesCache;
import org.apache.commons.vfs2.impl.DefaultFileReplicator;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.impl.SynchronizedFileObject;
import org.apache.commons.vfs2.provider.DelegateFileObject;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.apache.commons.vfs2.provider.ram.RamFileProvider;
import org.apache.commons.vfs2.provider.url.UrlFileProvider;
import org.junit.Test;


public class DarcFileUtilTest
{
    @Test
    public void testUnwrapDarcFile() throws FileSystemException
    {
        DefaultFileSystemManager manager = getManager(); manager.init();
        FileObject backend = manager.resolveFile("ram:/");
        FileObject darc = manager.resolveFile("darc:ram:/!/");
        assertSame(darc, DarcFileUtil.unwrapDarcFile(darc));
        assertNull(DarcFileUtil.unwrapDarcFile(null));
        assertNull(DarcFileUtil.unwrapDarcFile(backend));
    }

    @Test
    public void testUnwrapDarcFileJunction() throws FileSystemException
    {
        DefaultFileSystemManager manager = getManager(); manager.init();

        FileSystemOptions fsoptions = new FileSystemOptions();
        DarcFileConfigBuilder config = DarcFileConfigBuilder.getInstance();
        config.setChangeSession(fsoptions, "change");

        FileObject darc = manager.resolveFile("darc:ram:/!/", fsoptions);
        FileObject test = darc.resolveFile("test"); test.createFolder();
        FileObject vfsRoot = manager.createVirtualFileSystem("vfs://");
        FileSystem vfs = vfsRoot.getFileSystem();
        vfs.addJunction("test", darc.resolveFile("test"));
        FileObject del = vfsRoot.resolveFile("test");
        assertTrue(del instanceof DelegateFileObject);
        assertTrue(DarcFileUtil.unwrapDarcFile(del) instanceof DarcFileObject);
        assertSame(test, DarcFileUtil.unwrapDarcFile(del));
    }

    @Test
    public void testUnwrapDarcFileSyncJunction() throws FileSystemException
    {
        DefaultFileSystemManager manager = getManager();
        manager.setFileObjectDecorator(SynchronizedFileObject.class); manager.init();

        FileSystemOptions fsoptions = new FileSystemOptions();
        DarcFileConfigBuilder config = DarcFileConfigBuilder.getInstance();
        config.setChangeSession(fsoptions, "change");

        FileObject darc = manager.resolveFile("darc:ram:/!/", fsoptions);
        FileObject test = darc.resolveFile("test"); test.createFolder();
        FileObject vfsRoot = manager.createVirtualFileSystem("vfs://");
        FileSystem vfs = vfsRoot.getFileSystem();
        vfs.addJunction("test", darc.resolveFile("test"));
        FileObject del = vfsRoot.resolveFile("test");
        assertFalse(del instanceof DarcFileObject);
        assertTrue(test instanceof SynchronizedFileObject);
        assertNotNull(del);
        assertTrue(DarcFileUtil.unwrapDarcFile(del) instanceof DarcFileObject);
        assertTrue(DarcFileUtil.unwrapDarcFile(test) instanceof DarcFileObject);
        assertSame(DarcFileUtil.unwrapDarcFile(test), DarcFileUtil.unwrapDarcFile(del));
    }


    /** creates a new, uninitialized DefaultFileSystemManager. */
    private DefaultFileSystemManager getManager() throws FileSystemException
    {
        DefaultFileSystemManager manager = new DefaultFileSystemManager();
        manager.addProvider("file", new DefaultLocalFileProvider());
        manager.addProvider("ram", new RamFileProvider());
        manager.addProvider("darc", new DarcFileProvider());

        manager.setCacheStrategy(CacheStrategy.MANUAL);
        manager.setFilesCache(new DefaultFilesCache());

        manager.setDefaultProvider(new UrlFileProvider());
        manager.setBaseFile(manager.resolveFile("ram:/"));

        File tmp = new File(System.getProperty("java.io.tmpdir"), "vfslocal");
        tmp.mkdirs();
        manager.setReplicator(new DefaultFileReplicator(tmp));

        return manager;
    }
}



