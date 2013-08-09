package com.seeburger.vfs2.provider.jdbctable;

import java.util.Collection;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileNotFoundException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;

public class JdbcTableFileSystem
    extends AbstractFileSystem
{
	String tableName;

    protected JdbcTableFileSystem(final FileName rootName, final FileSystemOptions opts)
    {
        super(rootName, null, opts);
        tableName = JdbcTableFileSystemConfigBuilder.getInstance().getTablename(opts);
    }

    /**
     * Adds the capabilities of this file system.
     */
    @Override
    protected void addCapabilities(final Collection<Capability> caps)
    {
        caps.addAll(JdbcTableProvider.capabilities);
    }

    @Override
    public void doCloseCommunicationLink()
    {
    }

    /**
     * Creates a file object.  This method is called only if the requested
     * file is not cached.
     */
    @Override
    protected FileObject createFile(final AbstractFileName name)
        throws Exception
    {
    	String path = name.getPath();
    	System.out.println("new file " + name + " (" + name.getPath() + ")");

    	if ("/".equals(path))
    		return new JdbcTableSpecialFile(name, this);

    	if ("/key".equals(path))
    		return new JdbcTableSpecialFile(name, this);

    	if (path.startsWith("/key/"))
    		return new JdbcTableRowFile(name, this);

    	throw new FileNotFoundException(name);
    }

}
