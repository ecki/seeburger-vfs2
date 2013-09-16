/*
 * SimpleFilesystemTest.java
 *
 * created at 16.09.2013 by Eckenfel <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;

import static org.junit.Assert.*;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;


public class SimpleFilesystemTest
{

    @AfterClass
    public static void tearDownAfterClass()
        throws Exception
    {}


    @Before
    public void setUp()
        throws Exception
    {}


    @After
    public void tearDown()
        throws Exception
    {}


    @Test
    public void test() throws FileSystemException
    {
        DefaultFileSystemManager manager = new DefaultFileSystemManager();
        manager.addProvider("seearc", new DarcFileProvider());
        manager.addProvider("file", new DefaultLocalFileProvider());
        manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
        manager.init();
    }

}



