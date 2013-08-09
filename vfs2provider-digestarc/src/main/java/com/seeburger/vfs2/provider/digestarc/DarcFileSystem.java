package com.seeburger.vfs2.provider.digestarc;

import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.apache.commons.vfs2.provider.DelegateFileObject;

import com.seeburger.vfs2.provider.digestarc.DarcFile.File;

public class DarcFileSystem extends AbstractFileSystem
{
	DarcFile tree;
	private String blobStore;

	public DarcFileSystem(final AbstractFileName rootName,
			final FileObject parentLayer,
			final FileSystemOptions fileSystemOptions)
					throws FileSystemException
	{
		super(rootName, parentLayer, fileSystemOptions);
		blobStore = "file:///C:/temp/objects";

		// Make a local copy of the darcFile
		//File darcFile = parentLayer.getFileSystem().replicateFile(parentLayer, Selectors.SELECT_SELF);

		// Open the Zip darcFile
		/*if (!darcFile.exists())
		{
			// Don't need to do anything
			return;
		}*/
		// zipFile = createZipFile(this.file);
	}

	@Override
	public void init() throws FileSystemException
	{
		super.init();


		try
		{
			tree = new DarcFile();
		}
		finally
		{
			closeCommunicationLink();
		}
	}


	@Override
	protected void doCloseCommunicationLink()
	{
		// Release the zip darcFile
	}

	/**
	 * Returns the capabilities of this darcFile system.
	 */
	@Override
	protected void addCapabilities(final Collection<Capability> caps)
	{
		caps.addAll(DarcFileProvider.capabilities);
	}

	/**
	 * Creates a darcFile object.
	 */
	@Override
	protected FileObject createFile(final AbstractFileName name) throws FileSystemException
	{
		DarcFile.Entry entry = tree.resolveName(name.getPath());
		if (entry instanceof DarcFile.Directory)
		{
			return new DarcFileObject(name, this, entry);
		}
		if (entry instanceof DarcFile.File)
		{
			return new DarcFileObjectDelegate(name, this, (File)entry);
		}
		throw new FileSystemException("file not found", name);
	}
}
