/*
 * PostgreSQLProviderTestCase.java
 *
 * created at 2018-07-30 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable.test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Locale;

import javax.sql.DataSource;

import junit.framework.Test;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.AbstractProviderTestConfig;
import org.apache.commons.vfs2.ProviderTestSuite;
import org.postgresql.ds.PGSimpleDataSource;

import com.googlecode.flyway.core.Flyway;
import com.seeburger.vfs2.provider.jdbctable.JdbcDialectPostgreSQL;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;


public class PostgreSQLProviderTestCase extends AbstractProviderTestConfig
{
    private static DataSource dataSource;
    private boolean inited;


    @Override
    public void prepare(DefaultFileSystemManager manager)
        throws Exception
    {
        if (!manager.hasProvider("seejt"))
        {
            manager.addProvider("seejt", new JdbcTableProvider(new JdbcDialectPostgreSQL(dataSource)));
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
            FileObject from = getReadFolder();
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

        PGSimpleDataSource ds = new org.postgresql.ds.PGSimpleDataSource(); // slow
        ds.setUser("postgres");
        ds.setPassword("mysecretpassword");
        //ds.setServerName("10.15.79.226"); ds.setPortNumber(4432);
        ds.setDatabaseName("postgres");
        //ds.setAutoCommit(false);

        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        flyway.setLocations("db/migration/postgresql");
        flyway.setCleanOnValidationError(true);
        flyway.migrate();

        Connection c = ds.getConnection();
        System.out.println("Metadata: " + c.getMetaData().getDatabaseProductName() + " " +  c.getMetaData().getDatabaseProductVersion());
        assertTrue("must contain dialec", c.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql"));
        c.setAutoCommit(true);
        PreparedStatement ps = c.prepareStatement("DELETE FROM tBlobs");
        ps.executeUpdate();
        c.close();

        dataSource = ds;

        return new ProviderTestSuite(new PostgreSQLProviderTestCase(), true);
    }
}



