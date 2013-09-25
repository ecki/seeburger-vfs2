package com.seeburger.vfs2.provider.jdbctable;


import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileNotFoundException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;


public class JdbcTableFileSystem extends AbstractFileSystem
{
    final JdbcDialect dialect;
    final JdbcTableProvider provider;
    final boolean writeMode;

    protected JdbcTableFileSystem(final FileName rootName,
                                  final JdbcTableProvider jdbcTableProvider,
                                  final FileSystemOptions fileSystemOptions)
    {
        super(rootName, /*parentlayer*/null, fileSystemOptions);
        this.provider = jdbcTableProvider;
        JdbcTableFileSystemConfigBuilder config = JdbcTableFileSystemConfigBuilder.getInstance();
        this.writeMode = config.getWriteMode(fileSystemOptions);
        this.dialect = jdbcTableProvider.getDialectForTable(config.getTablename(fileSystemOptions));
    }

    /**
     * Adds the capabilities of this file system.
     */
    @Override
    protected void addCapabilities(final Collection<Capability> caps)
    {
        caps.addAll(JdbcTableProvider.capabilities);
        // do we have the capability to change things?
        if (writeMode)
        {
            caps.addAll(JdbcTableProvider.writeCapabilities);
        }
    }

    @Override
    public void doCloseCommunicationLink()
    {
//System.out.println("doCloseCommunicationLink() " + toString());
    }

    /**
     * Creates a file object.  This method is called only if the requested
     * file is not cached.
     */
    @Override
    protected FileObject createFile(final AbstractFileName name)
                    throws Exception
    {
        return new JdbcTableRowFile(name, this);
    }

    public JdbcDialect getDialect()
    {
        return dialect;
    }
}
