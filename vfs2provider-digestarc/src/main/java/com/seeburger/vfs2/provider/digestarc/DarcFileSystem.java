package com.seeburger.vfs2.provider.digestarc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.apache.commons.vfs2.provider.LayeredFileName;

import com.seeburger.vfs2.provider.digestarc.DarcTree.Entry;

public class DarcFileSystem extends AbstractFileSystem
{
	DarcTree tree;
	private BlobStorageProvider provider;
	String rootHash;

	public DarcFileSystem(final LayeredFileName rootName,
			final FileObject parentLayer,
			final FileSystemOptions fileSystemOptions)
					throws FileSystemException
	{
		super(rootName, parentLayer, fileSystemOptions);
		// the filesystem is layered on top of the root tree, blobs are relative to ../.. there
		FileName pool = rootName.getOuterName().getParent().getParent();
		String refName = pool.getRelativeName(rootName.getOuterName());
		rootHash = refName.replaceFirst("/", "");
		provider = new BlobStorageProvider(this, pool);
	}

	@Override
	public void init() throws FileSystemException
	{
		super.init();

		InputStream is = null;
		try
		{
		    FileObject refFile = provider.resolveFileHash(rootHash);
		    is = refFile.getContent().getInputStream();
			try
            {
                tree = new DarcTree(is, rootHash);
            }
            catch (IOException e)
            {
                throw new FileSystemException(e);
            }
		}
		finally
		{
		    try { if (is != null) is.close(); } catch (Exception ignored) { }
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
	 * @throws IOException
	 */
	@Override
	protected FileObject createFile(final AbstractFileName name) throws IOException
	{
	    System.out.println("creating " + name.getPathDecoded());
	    Entry entry = tree.resolveName(name.getPathDecoded(), provider);
	    return new DarcFileObject(name, this, entry);
	}

    public BlobStorageProvider getBlobProvider()
    {
        return provider;
    }
}
