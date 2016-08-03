/*
 * DarcProviderTestCase.java
 *
 * created at 2013-10-15 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;

import java.io.File;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.ram.RamFileProvider;
import org.apache.commons.vfs2.test.AbstractProviderTestConfig;
import org.apache.commons.vfs2.test.ProviderTestConfig;

import junit.framework.Test;


public class DarcProviderTestCase extends AbstractProviderTestConfig implements ProviderTestConfig
{
    private FileSystemOptions opts;
    private boolean inited;


    @Override
    public void prepare(DefaultFileSystemManager manager)
        throws Exception
    {
        if (!manager.hasProvider("ram"))
        {
            manager.addProvider("ram", new RamFileProvider());
        }
        if (!manager.hasProvider("darc"))
        {
            manager.addProvider("darc", new DarcFileProvider());
        }
    }

    /**
     * Returns the base folder for tests.
     */
    @Override
    public FileObject getBaseTestFolder(final FileSystemManager manager)
            throws Exception
    {
        if (!inited)
        {
            opts = new FileSystemOptions();
            final DarcFileConfigBuilder builder = DarcFileConfigBuilder.getInstance();
            builder.setChangeSession(opts, "providertest");

            // Import the test tree
            FileObject base = manager.resolveFile("darc:ram:/!/test-data", opts);

            // fs.importTree(getTestDirectory());
            FileObject from = manager.resolveFile(new File("."), getTestDirectory());
            base.copyFrom(from, new AllFileSelector());
            base.resolveFile("read-tests/emptydir").createFolder();

            FileObject fo = manager.resolveFile("darc:ram:/!/test-data/code", opts);
            from = manager.resolveFile(new File("."), "target/test-classes/code");
            fo.copyFrom(from, new AllFileSelector());

            inited=true;

            //TreePrinter.printTree(base, "| ", System.out);
        }

        return manager.resolveFile("darc:ram:/!/test-data", opts);
    }


    /**
     * Creates the test suite for the ram file system.
     */
    public static Test suite() throws Exception
    {
        System.setProperty("test.basedir",  "../src/test/test-data");
        return new DarcProviderTestSuite(new DarcProviderTestCase());
    }
}



