/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.seeburger.vfs2.provider.jdbctable.test;

import java.io.IOException;

import junit.framework.Assert;
import junit.framework.Test;

import org.apache.commons.vfs2.FileNotFolderException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.http.HttpFileProvider;
import org.apache.commons.vfs2.test.AbstractProviderTestConfig;
import org.apache.commons.vfs2.test.ProviderTestSuite;

import com.seeburger.vfs2.provider.jdbctable.JdbcTableFileSystemConfigBuilder;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;

/**
 * Test cases for the HTTP provider.
 *
 */
public class JdbcTableProviderTestCase extends AbstractProviderTestConfig
{
    private static int SocketPort;

    private static final String TEST_URI = "test.http.uri";

    /**
     * Use %40 for @ in URLs
     */
    private static String ConnectionUri;

    private static String getSystemTestUriOverride()
    {
        return System.getProperty(TEST_URI);
    }

    /**
     * Creates and starts an embedded Apache HTTP Server (HttpComponents).
     *
     * @throws Exception
     */
    private static void setUpClass() throws Exception
    {
    }

    /**
     * Creates a new test suite.
     *
     * @return a new test suite.
     * @throws Exception
     *             Thrown when the suite cannot be constructed.
     */
    public static Test suite() throws Exception
    {
        return new ProviderTestSuite(new JdbcTableProviderTestCase())
        {
            /**
             * Adds base tests - excludes the nested test cases.
             */
            @Override
            protected void addBaseTests() throws Exception
            {
                super.addBaseTests();
                addTests(JdbcTableProviderTestCase.class);
            }

            @Override
            protected void setUp() throws Exception
            {
                if (getSystemTestUriOverride() == null)
                {
                    setUpClass();
                }
                super.setUp();
            }

            @Override
            protected void tearDown() throws Exception
            {
                tearDownClass();
                super.tearDown();
            }
        };
    }

    /**
     * Stops the embedded Apache HTTP Server.
     *
     * @throws IOException
     */
    private static void tearDownClass() throws IOException
    {
    }

    /**
     * Builds a new test case.
     *
     * @throws IOException
     *             Thrown if a free local socket port cannot be found.
     */
    public JdbcTableProviderTestCase() throws IOException
    {
        //SocketPort = FreeSocketPortUtil.findFreeLocalPort();
        // Use %40 for @ in a URL
        //ConnectionUri = "http://localhost:" + SocketPort;
    }

    private void checkReadTestsFolder(final FileObject file) throws FileSystemException
    {
        Assert.assertNotNull(file.getChildren());
        Assert.assertTrue(file.getChildren().length > 0);
    }

    /**
     * Returns the base folder for tests.
     */
    @Override
    public FileObject getBaseTestFolder(final FileSystemManager manager) throws Exception
    {
        String uri = getSystemTestUriOverride();
        if (uri == null)
        {
            uri = ConnectionUri;
        }
        return manager.resolveFile(uri);
    }

    /**
     * Prepares the file system manager.
     */
    @Override
    public void prepare(final DefaultFileSystemManager manager) throws Exception
    {
        manager.addProvider("seejt", new JdbcTableProvider());
    }

}
