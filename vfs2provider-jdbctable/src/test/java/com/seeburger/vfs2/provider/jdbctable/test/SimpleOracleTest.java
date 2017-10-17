/*
 * SimpleOracleTest.java
 *
 * created at 2013-09-09 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.jdbctable.test;


import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.googlecode.flyway.core.Flyway;
import com.seeburger.vfs2.provider.jdbctable.JdbcDialectBase;
import com.seeburger.vfs2.provider.jdbctable.JdbcDialectOracle;


public class SimpleOracleTest extends SimpleTestsBase
{
    static boolean skip = false;

    @BeforeClass
    public static void setupDatabase() throws SQLException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException, ClassNotFoundException
    {
        System.out.println("Connecting Oracle database");

        // dynamically construct Oracle Data Source since this is a properitary dependency
        DataSource ds = (DataSource)Class.forName("oracle.jdbc.pool.OracleDataSource").getConstructor((Class<?>)null).newInstance((Object[])null);
        PropertyUtils.setSimpleProperty(ds, "user", "TESTVFS"); // ds.setUser("TESTVFS");
        PropertyUtils.setSimpleProperty(ds, "password", "secret"); // ds.setPassword("secret");
        PropertyUtils.setSimpleProperty(ds, "URL", "jdbc:oracle:thin:@localhost:1521:ORA1"); // ds.setURL("...");

        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        flyway.setLocations("db/migration/oracle");
        flyway.setValidateOnMigrate(true);
        flyway.setCleanOnValidationError(true);
        flyway.migrate();

        System.out.println("Tables in this schema");
        Connection c = ds.getConnection();
        DatabaseMetaData md = c.getMetaData();
        ResultSet rs = md.getTables(null, "TESTVFS", null, new String[] { "TABLE" });
        while(rs.next())
        {
            System.out.println(rs.getString(1)+"|"+rs.getString(2)+"|"+rs.getString(3)+"|"+rs.getString(4));
        }
        rs.close(); c.close();

        SimpleTestsBase.dataSource = ds;
        SimpleTestsBase.dialect = JdbcDialectBase.getDialect(dataSource);

        assertEquals(JdbcDialectOracle.class.getName(), dialect.getClass().getName());
    }

    @AfterClass
    public static void destroyDatabase()
    {
        SimpleTestsBase.dataSource = null;
    }

    void verifyDatabase()
    {
        /* Since Oracle Database is shared I wont check for left-open connections */
    }
}



