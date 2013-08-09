package com.seeburger.vfs2.provider.jdbctable;

import javax.sql.DataSource;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

/**
 * Configuration options for SEEBURGER Jdbc Table.
 */
public class JdbcTableFileSystemConfigBuilder extends FileSystemConfigBuilder
{
    private static final JdbcTableFileSystemConfigBuilder BUILDER = new JdbcTableFileSystemConfigBuilder();

    protected JdbcTableFileSystemConfigBuilder(final String prefix)
    {
        super(prefix);
    }

    private JdbcTableFileSystemConfigBuilder()
    {
        super("seejt.");
    }

    public static JdbcTableFileSystemConfigBuilder getInstance()
    {
        return BUILDER;
    }

    public void setDatasource(final FileSystemOptions opts, final DataSource dataSource)
    {
        setParam(opts, "datasource", dataSource);
    }

    public String getDatasource(final FileSystemOptions opts)
    {
        return getString(opts, "datasource", "java:/DataSource");
    }

    public void setTablename(final FileSystemOptions opts, final String name)
    {
        setParam(opts, "table", name);
    }

    public String getTablename(final FileSystemOptions opts)
    {
        return getString(opts, "table", "tFilesystem");
    }

    @Override
    protected Class<? extends FileSystem> getConfigClass()
    {
        return JdbcTableFileSystem.class;
    }
}
