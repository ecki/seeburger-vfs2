/*
 * DarcProviderTestSuite.java
 *
 * created at 2013-10-15 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import junit.framework.Test;

import org.apache.commons.vfs2.impl.test.VfsClassLoaderTests;
import org.apache.commons.vfs2.test.ContentTests;
import org.apache.commons.vfs2.test.LastModifiedTests;
import org.apache.commons.vfs2.test.NamingTests;
import org.apache.commons.vfs2.test.ProviderDeleteTests;
import org.apache.commons.vfs2.test.ProviderRandomReadTests;
import org.apache.commons.vfs2.test.ProviderRandomReadWriteTests;
import org.apache.commons.vfs2.test.ProviderReadTests;
import org.apache.commons.vfs2.test.ProviderRenameTests;
import org.apache.commons.vfs2.test.ProviderTestConfig;
import org.apache.commons.vfs2.test.ProviderTestSuite;
import org.apache.commons.vfs2.test.ProviderWriteAppendTests;
import org.apache.commons.vfs2.test.ProviderWriteTests;
import org.apache.commons.vfs2.test.UriTests;
import org.apache.commons.vfs2.test.UrlStructureTests;
import org.apache.commons.vfs2.test.UrlTests;


/** DarcProvider is tested with apache base test suite sans ProviderCacheStrategyTest. */
public class DarcProviderTestSuite
    extends ProviderTestSuite
    implements Test
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
