package com.seeburger.vfs2.provider.jdbctable;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.commons.vfs2.FileContentInfoFactory;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.util.RandomAccessMode;

public class JdbcTableRowFile extends AbstractFileObject
{
	private FileType type;

	public JdbcTableRowFile(AbstractFileName name, JdbcTableFileSystem fs)
	{
		super(name, fs);
		type = FileType.IMAGINARY;
	}

	@Override
	protected void doAttach() throws Exception
	{
		System.out.println("Attaching " + getName());
		if (getName().getBaseName().equals("a1"))
			type = FileType.FILE;
		else
			type = FileType.IMAGINARY;
	}

	@Override
	protected void doDetach() throws Exception {
		System.out.println("Detaching " + getName());
	}

	@Override
	protected long doGetLastModifiedTime() throws Exception {
		// TODO Auto-generated method stub
		return super.doGetLastModifiedTime();
	}

	@Override
	protected boolean doSetLastModifiedTime(long modtime) throws Exception {
		// TODO Auto-generated method stub
		return super.doSetLastModifiedTime(modtime);
	}

	@Override
	protected Map<String, Object> doGetAttributes() throws Exception {
		// TODO Auto-generated method stub
		return super.doGetAttributes();
	}

	@Override
	protected void doSetAttribute(String attrName, Object value)
			throws Exception {
		// TODO Auto-generated method stub
		super.doSetAttribute(attrName, value);
	}

	@Override
	protected RandomAccessContent doGetRandomAccessContent(RandomAccessMode mode)
			throws Exception {
		// TODO Auto-generated method stub
		return super.doGetRandomAccessContent(mode);
	}

	@Override
	protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
		// TODO Auto-generated method stub
		return super.doGetOutputStream(bAppend);
	}

	@Override
	protected FileType doGetType() throws Exception
	{
		return type;
	}

	@Override
	protected String[] doListChildren() throws Exception
	{
		return null;
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

    @Override
    protected FileContentInfoFactory getFileContentInfoFactory()
    {
    	System.out.println("getFileContentInfoFactory");
        return new JdbcTableContentInfoFactory(); // TODO: singleton
    }

	@Override
	public FileObject getChild(String file) throws FileSystemException
	{
        FileSystem fs = getFileSystem();
		FileName children = fs.getFileSystemManager().resolveName(getName(), file, NameScope.CHILD);
		return fs.resolveFile(children);
	}

}
