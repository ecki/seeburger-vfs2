/*
 * MSSQLProviderTestCase.java
 *
 * created at 2013-09-13 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable.test;


import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import junit.framework.Test;
import net.sourceforge.jtds.jdbcx.JtdsDataSource;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.test.AbstractProviderTestConfig;
import org.apache.commons.vfs2.test.ProviderTestConfig;
import org.apache.commons.vfs2.test.ProviderTestSuite;

import com.googlecode.flyway.core.Flyway;
import com.seeburger.vfs2.provider.jdbctable.JdbcDialectMSSQL;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;


public class MSSQLProviderTestCase extends AbstractProviderTestConfig implements ProviderTestConfig
{
    private static DataSource dataSource;
    private boolean inited;


    @Override
    public void prepare(DefaultFileSystemManager manager)
        throws Exception
    {
        if (!manager.hasProvider("seejt"))
        {
            manager.addProvider("seejt", new JdbcTableProvider(new JdbcDialectMSSQL(dataSource)));
        } else {
            System.out.println("Already has provider seejt");
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
            // Import the test tree
            FileObject base = manager.resolveFile("seejt:/key/test-data");

            // fs.importTree(getTestDirectory());
            FileObject from = manager.resolveFile(new File("."), getTestDirectory());
            base.copyFrom(from, new AllFileSelector());
            base.resolveFile("read-tests/emptydir").createFolder();

            FileObject fo = manager.resolveFile("seejt:/key/test-data/code");
            from = manager.resolveFile(new File("."), "target/test-classes/code");
            fo.copyFrom(from, new AllFileSelector());

            inited=true;
        }

        return manager.resolveFile("seejt:/key/test-data");
    }


    /**
     * Creates the test suite for the ram file system.
     */
    public static Test suite() throws Exception
    {
        System.setProperty("test.basedir",  "../src/test/test-data");

        JtdsDataSource ds = new JtdsDataSource();
        ds.setUser("VFSTEST");
        ds.setPassword("secret");
        ds.setServerName("127.0.0.1"); ds.setPortNumber(49762);
        ds.setDatabaseName("VFSTEST");
        ds.setAutoCommit(false);
        ds.setMacAddress("010203040506"); // 20x speedup

        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        flyway.setLocations("db/migration/mssql");
        flyway.setCleanOnValidationError(true);
        flyway.migrate();

        Connection c = ds.getConnection();
        PreparedStatement ps = c.prepareStatement("DELETE FROM tBlobs");
        ps.executeUpdate();
        c.commit();
        c.close();

        dataSource = ds;

        return new ProviderTestSuite(new MSSQLProviderTestCase(), true);
    }
}



