package com.seeburger.vfs2.provider.jdbctable;

import java.io.InputStream;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;

public class JdbcTableSpecialFile extends AbstractFileObject {

	public JdbcTableSpecialFile(AbstractFileName name, JdbcTableFileSystem fs)
	{
		super(name, fs);
	}

	@Override
	protected FileType doGetType() throws Exception
	{
		return FileType.FOLDER;
	}

	@Override
	protected String[] doListChildren() throws Exception
	{
		if ("/".equals(getName().getPath()))
			return new String[] { "/key" };
		throw new RuntimeException("Cannot list all children of " + getName());
	}

	@Override
	public FileObject getChild(String file) throws FileSystemException
	{
		if ("/".equals(getName().getPath()))
			return super.getChild(file);

		FileSystem fs = getFileSystem();
		FileName children = fs.getFileSystemManager().resolveName(getName(), file, NameScope.CHILD);
		return fs.resolveFile(children);
	}

	@Override
	protected long doGetContentSize() throws Exception{
		System.out.println("doGetContentSize" + getName());
		return 0;
	}

	@Override
	protected InputStream doGetInputStream() throws Exception
	{
		System.out.println("doGetInputStream " + getName());
		return null;
	}

}
