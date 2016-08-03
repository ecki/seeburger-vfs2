/*
 * StandaloneClient.java
 *
 * created at 2013-08-09 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable.test;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.derby.jdbc.EmbeddedDataSource;

import com.googlecode.flyway.core.Flyway;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;
import com.seeburger.vfs2.util.TreePrinter;


public class StandaloneClient
{
    public static void main(String[] args) throws SQLException, IOException
    {
        final String PREFIX = "seejt:///test-data";
        DataSource ds = createDatasource();
        FileSystemManager manager = createManager(ds);

        final FileObject base = manager.resolveFile(PREFIX);
        final FileObject root = base.getFileSystem().getRoot();

        System.out.println("base=" + base + " rootFile=" + root + " rootURI=" + base.getFileSystem().getRootURI()) ;

        TreePrinter.printTree(root , "|", System.out);

        System.out.println("-- resolving a1+b2");
        FileObject a1 = manager.resolveFile(PREFIX + "/a1");
        FileObject b2 = manager.resolveFile(PREFIX + "/b2");
        TreePrinter.printTree(a1 , "a1> ", System.out);
        TreePrinter.printTree(b2 , "b2> ", System.out);

        System.out.println("-- resolving again");
        a1 = manager.resolveFile(base, "a1");
        b2 = manager.resolveFile(base, "b2");
        TreePrinter.printTree(a1 , "a1> ", System.out);
        TreePrinter.printTree(b2 , "b2> ", System.out);

        System.out.println("-- getting named child a1+b2");
        a1 = base.getChild("a1");
        b2 = base.getChild("b2");
        TreePrinter.printTree(a1 , "a1> ", System.out);
        TreePrinter.printTree(b2 , "b2> ", System.out);

        System.out.println("-- refreshing");
        a1.refresh();
        b2.refresh();
        TreePrinter.printTree(a1 , "a1> ", System.out);
        TreePrinter.printTree(b2 , "b2> ", System.out);

        int count = 1000;
        FileObject ax = null;
        long now = System.currentTimeMillis();
        long start = System.nanoTime();

        System.out.println("Reading ("+count+") ...");
        for(int i=0; i<count; i++)
        {
            ax = manager.resolveFile(PREFIX + "/abcd_" + now + "_" +  i);
            //ax.createFile();
            OutputStream os = ax.getContent().getOutputStream();
            os.write(1); os.write(2); os.write(3); os.close();
            ax = manager.resolveFile(ax.toString());
            if (i % 200 == 0)
                TreePrinter.printTree(ax ,String.format("(%3d) ", Integer.valueOf(i)), System.out);
        }

        long middle = System.nanoTime();
        System.out.printf("Write Time: %,.3f ms.%n", Double.valueOf((middle - start) / 1000000.0));

        System.out.println("Reading ("+count+") ...");
        for(int i=0; i<count; i++)
        {
            ax = manager.resolveFile(PREFIX + "/abcd_" + now + "_"+  i);
            InputStream is = ax.getContent().getInputStream();
            if (is.read() != 1 || is.read() != 2 || is.read() != 3)
                System.out.println("not 1 2 3");
            is.close();
            if (i % 200 == 0)
                TreePrinter.printTree(ax ,String.format("(%3d) ", Integer.valueOf(i)), System.out);
        }

        long end = System.nanoTime();
        System.out.printf("Read Time: %,.3f ms. Have a good time.%n", Double.valueOf((end - middle) / 1000000.0));

        System.out.println("Destroying test files...");
        base.delete(Selectors.EXCLUDE_SELF);

        System.out.println("StandaloneClient done.");
    }

    private static FileSystemManager createManager(DataSource ds) throws FileSystemException
    {
        DefaultFileSystemManager manager = new DefaultFileSystemManager();
        manager.addProvider("seejt", new JdbcTableProvider(ds)); // detects dialect automatically
        manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
        manager.init();

        return manager;
    }

    private static DataSource createDatasource() throws SQLException
    {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setUser("SEEASOWN");
        ds.setPassword("secret");
        ds.setCreateDatabase("create");
        ds.setDatabaseName("target/SimpleDerbyTestDB");

        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        flyway.setLocations("db/migration/h2_derby");
        flyway.setValidateOnMigrate(true);
        flyway.setCleanOnValidationError(true);
        flyway.migrate();

        ds = new EmbeddedDataSource();
        ds.setUser("SEEASOWN");
        ds.setPassword("secret");
        ds.setCreateDatabase("false"); // otherweise we get SQLWarnings
        ds.setDatabaseName("target/SimpleDerbyTestDB");

        return ds;
    }
}
