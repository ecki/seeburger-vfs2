/*
 * DarcProviderTestSuite.java
 *
 * created at 2013-10-15 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;

import org.apache.commons.vfs2.impl.VfsClassLoaderTests;
import org.apache.commons.vfs2.ContentTests;
import org.apache.commons.vfs2.LastModifiedTests;
import org.apache.commons.vfs2.NamingTests;
import org.apache.commons.vfs2.ProviderDeleteTests;
import org.apache.commons.vfs2.ProviderRandomReadTests;
import org.apache.commons.vfs2.ProviderRandomReadWriteTests;
import org.apache.commons.vfs2.ProviderReadTests;
import org.apache.commons.vfs2.ProviderRenameTests;
import org.apache.commons.vfs2.ProviderTestConfig;
import org.apache.commons.vfs2.ProviderTestSuite;
import org.apache.commons.vfs2.ProviderWriteAppendTests;
import org.apache.commons.vfs2.ProviderWriteTests;
import org.apache.commons.vfs2.UriTests;
import org.apache.commons.vfs2.UrlStructureTests;
import org.apache.commons.vfs2.UrlTests;


/** DarcProvider is tested with apache base test suite sans ProviderCacheStrategyTest. */
public class DarcProviderTestSuite extends ProviderTestSuite
{
    public DarcProviderTestSuite(ProviderTestConfig testConfig) throws Exception
    {
        super(testConfig, true);
    }

    @Override
    protected void addBaseTests()
        throws Exception
    {
        // copy list, remove cace test - does not work with RAM FS
        // addTests(ProviderCacheStrategyTests.class);
        addTests(UriTests.class);
        addTests(NamingTests.class);
        addTests(ContentTests.class);
        addTests(ProviderReadTests.class);
        addTests(ProviderRandomReadTests.class);
        addTests(ProviderWriteTests.class);
        addTests(ProviderWriteAppendTests.class);
        addTests(ProviderRandomReadWriteTests.class);
        addTests(ProviderRenameTests.class);
        addTests(ProviderDeleteTests.class);
        addTests(LastModifiedTests.class);
        addTests(UrlTests.class);
        addTests(UrlStructureTests.class);
        addTests(VfsClassLoaderTests.class);
    }
}
