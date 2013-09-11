package com.seeburger.vfs2.provider.jdbctable.test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;

import junit.framework.Test;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.test.AbstractProviderTestConfig;
import org.apache.commons.vfs2.test.ProviderTestConfig;
import org.apache.commons.vfs2.test.ProviderTestSuite;
import org.apache.derby.jdbc.EmbeddedDataSource;

import com.googlecode.flyway.core.Flyway;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;


public class DerbyProviderTestCase extends AbstractProviderTestConfig implements ProviderTestConfig
{
    private static EmbeddedDataSource dataSource;
    private boolean inited;


    @Override
    public void prepare(DefaultFileSystemManager manager)
        throws Exception
    {
        if (!manager.hasProvider("seejt"))
        {
            manager.addProvider("seejt", new JdbcTableProvider(dataSource));
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
            FileObject fo = manager.resolveFile("seejt:/key/test-data/code");
            from = manager.resolveFile(new File("."), "target/test-classes/code");
            fo.copyFrom(from, new AllFileSelector());

            inited=true;

            StandaloneClient.listFiles("|", base);
        }

        return manager.resolveFile("seejt:/key/test-data");
    }


    /**
     * Creates the test suite for the ram file system.
     */
    public static Test suite() throws Exception
    {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setUser("SEEASOWN");
        ds.setPassword("secret");
        ds.setCreateDatabase("create");
        ds.setDatabaseName("target/ProviderTestCaseDB");

        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        flyway.setLocations("db/migration/h2_derby");
        flyway.setCleanOnValidationError(true);
        flyway.migrate();

        ds = new EmbeddedDataSource();
        ds.setUser("SEEASOWN");
        ds.setPassword("secret");
        ds.setDatabaseName("target/ProviderTestCaseDB");

        Connection c = ds.getConnection();
        PreparedStatement ps = c.prepareStatement("DELETE FROM tBlobs");
        ps.executeUpdate();
        c.commit();
        c.close();

        dataSource = ds;

        return new ProviderTestSuite(new DerbyProviderTestCase());
    }
}



