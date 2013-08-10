package com.seeburger.vfs2.provider.digestarc;

import java.io.InputStream;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;

import com.seeburger.vfs2.provider.digestarc.DarcFile.Directory;
import com.seeburger.vfs2.provider.digestarc.DarcFile.Entry;
import com.seeburger.vfs2.provider.digestarc.DarcFile.File;

public class DarcFileObject extends AbstractFileObject
{
    private FileType type = FileType.IMAGINARY;

    private DarcFile.Directory darcDirectory;
	private DarcFile.File darcFile;

    protected DarcFileObject(final AbstractFileName name, final DarcFileSystem fs, Entry entry)
    		throws FileSystemException
    {
        super(name, fs);
        if (entry instanceof Directory)
        {
        	darcFile = null;
        	darcDirectory = (Directory)entry;
        	type = FileType.FOLDER;
        } else {
        	darcFile = (File)entry;
        	darcDirectory = null;
        	type = FileType.FILE;
        }
    }



    /**
     * Attaches a child.
     * <p/>
     * TODO: Shouldn't this method have package-only visibility?
     * Cannot change this without breaking binary compatibility.
     *
     * @param childName The name of the child.
     */
    /*public void attachChild(final FileName childName)
    {
        children.add(childName.getBaseName());
    }*/

    /**
     * Determines if this file can be written to.
     *
     * @return {@code true} if this file is writeable, {@code false} if not.
     * @throws FileSystemException if an error occurs.
     */
    @Override
    public boolean isWriteable() throws FileSystemException
    {
        return false;
    }

    /**
     * Returns the file's type.
     */
    @Override
    protected FileType doGetType()
    {
        return type;
    }

    /**
     * Lists the children of the file.
     */
    @Override
    protected String[] doListChildren()
    {
        try
        {
            if (!getType().hasChildren())
            {
                return null;
            }
        }
        catch (final FileSystemException e)
        {
            // should not happen as the type has already been cached.
            throw new RuntimeException(e);
        }

    	return darcDirectory.getChildrenNames();
    }

    @Override
    protected long doGetContentSize()
    {
        return darcFile.getSize();
    }

    /**
     * Returns the last modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime() throws Exception
    {
        return darcFile.getTime();
    }

    /**
     * Creates an input stream to read the file content from.  Is only called
     * if  {@link #doGetType} returns {@link FileType#FILE}.  The input stream
     * returned by this method is guaranteed to be closed before this
     * method is called again.
     */
    @Override
    protected InputStream doGetInputStream() throws Exception
    {
        // VFS-210: zip allows to gather an input stream even from a directory and will
        // return -1 on the first read. getType should not be expensive and keeps the tests
        // running
        if (!getType().hasContent())
        {
            throw new FileSystemException("vfs.provider/read-not-file.error", getName());
        }

        return null;
    }


	@Override
	protected void doAttach() throws Exception
	{
//System.out.println("attached " + getName() + " " + getName().getPath());
	}


	@Override
	protected void doDetach() throws Exception
	{
		darcDirectory = null;
		darcFile = null;
		type = FileType.IMAGINARY;
	}
}
