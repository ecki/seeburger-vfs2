/*
 * JdbcTableProviderTestCase.java
 *
 * created at 11.08.2013 by Eckenfel <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable.test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;

import junit.framework.Test;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.ram.RamFileSystem;
import org.apache.commons.vfs2.provider.ram.test.RamProviderTestCase;
import org.apache.commons.vfs2.test.AbstractProviderTestConfig;
import org.apache.commons.vfs2.test.ProviderTestConfig;
import org.apache.commons.vfs2.test.ProviderTestSuite;
import org.apache.derby.jdbc.EmbeddedDataSource40;

import com.googlecode.flyway.core.Flyway;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableFileSystem;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;


public class JdbcTableProviderTestCase extends AbstractProviderTestConfig implements ProviderTestConfig
{
    private boolean inited;


    @Override
    public void prepare(DefaultFileSystemManager manager)
        throws Exception
    {
        EmbeddedDataSource40 ds = new EmbeddedDataSource40();
        ds.setUser("SEEASOWN");
        ds.setPassword("secret");
        ds.setCreateDatabase("create");
        ds.setDatabaseName("target/ProviderTestCaseDB");

        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        flyway.setCleanOnValidationError(true);
        flyway.migrate();

        Connection c = ds.getConnection();
        PreparedStatement ps = c.prepareStatement("DELETE FROM tBlobs");
        ps.executeUpdate();
        c.commit();
        c.close();

        manager.addProvider("seejt", new JdbcTableProvider(ds));
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
            // Import the test tree
            FileObject fo = manager.resolveFile("seejt:/key/");
            final JdbcTableFileSystem  fs = (JdbcTableFileSystem) fo.getFileSystem();
            // fs.importTree(getTestDirectory());
            FileObject from = manager.resolveFile(new File("."), getTestDirectory());
            fo.copyFrom(from, new AllFileSelector());
            fo.close();

            fo = manager.resolveFile("seejt:/key/code");
            from = manager.resolveFile(new File("."), "target/test-classes/code");
            fo.copyFrom(from, new AllFileSelector());
            fo.close();
            inited=true;
        }

        return manager.resolveFile("seejt:/key/");
    }


    /**
     * Creates the test suite for the ram file system.
     */
    public static Test suite() throws Exception
    {
        return new ProviderTestSuite(new JdbcTableProviderTestCase());
    }
}



