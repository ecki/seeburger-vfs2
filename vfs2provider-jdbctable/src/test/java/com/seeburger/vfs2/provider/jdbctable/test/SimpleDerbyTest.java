/*
 * SimpleDerbyTest.java
 *
 * created at 2013-08-10 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable.test;


import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.googlecode.flyway.core.Flyway;
import com.seeburger.vfs2.provider.jdbctable.JdbcDialectBase;


public class SimpleDerbyTest extends SimpleTestsBase
{
    @BeforeClass
    public static void setupDatabase() throws SQLException
    {
        System.out.println("Creating Derby Database");
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setUser("VFSTEST");
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
        ds.setUser("VFSTEST");
        ds.setPassword("secret");
        ds.setDatabaseName("target/SimpleDerbyTestDB");

        SimpleTestsBase.dataSource = ds;
        SimpleTestsBase.dialect = JdbcDialectBase.getDialect(ds);

        assertEquals(JdbcDialectBase.class.getName(), dialect.getClass().getName());
    }

    @AfterClass
    public static void destroyDatabase()
    {
        System.out.println("Tearing down database");
        EmbeddedDataSource ds = (EmbeddedDataSource)dataSource; dataSource = null;
        ds.setShutdownDatabase("true");
    }

    @Override
    void verifyDatabase()
    {
        // TODO implement connection counting
    }
}



