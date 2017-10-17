/*
 * OracleProviderTestCase.java
 *
 * created at 2013-09-14 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable.test;


import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import junit.framework.Test;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.test.AbstractProviderTestConfig;
import org.apache.commons.vfs2.test.ProviderTestConfig;
import org.apache.commons.vfs2.test.ProviderTestSuite;

import com.googlecode.flyway.core.Flyway;
import com.seeburger.vfs2.provider.jdbctable.JdbcDialectOracle;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;


public class OracleProviderTestCase extends AbstractProviderTestConfig implements ProviderTestConfig
{
    private static DataSource dataSource;
    private boolean inited;


    @Override
    public void prepare(DefaultFileSystemManager manager)
        throws Exception
    {
        if (!manager.hasProvider("seejt"))
        {
            manager.addProvider("seejt", new JdbcTableProvider(new JdbcDialectOracle(dataSource)));
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

        // dynamically construct Oracle Data Source since this is a properitary dependency
        DataSource ds = (DataSource)Class.forName("oracle.jdbc.pool.OracleDataSource").getConstructor((Class<?>)null).newInstance((Object[])null);
        PropertyUtils.setSimpleProperty(ds, "user", "TESTVFS"); // ds.setUser("TESTVFS");
        PropertyUtils.setSimpleProperty(ds, "password", "secret"); // ds.setPassword("secret");
        PropertyUtils.setSimpleProperty(ds, "URL", "jdbc:oracle:thin:@localhost:1521:ORA1"); // ds.setURL("...");

        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        flyway.setLocations("db/migration/mssql");
        flyway.setCleanOnValidationError(true);
        flyway.migrate();

        Connection c = ds.getConnection();
        PreparedStatement ps = c.prepareStatement("DELETE FROM \"tBlobs\"");
        ps.executeUpdate();
        c.commit();
        c.close();

        dataSource = ds;

        return new ProviderTestSuite(new OracleProviderTestCase(), true);
    }
}



