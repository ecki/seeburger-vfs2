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


/**
 * Implements the Digest Archive file system which can be
 * layered on top of another VFS2 file-system to provide
 * a Git-like versioned directory.
 * <P>
 * The parent file needs to point to a DarfTree Object
 * containing the root of the filesystem. This is
 * an immutable file with hashed name.
 */
public class DarcFileSystem extends AbstractFileSystem
{
    /** In memory structure of this graph. */
    DarcTree tree;
    /** The provider responsible for looking up hashed names. */
    private BlobStorageProvider provider;
    /** The hash of the initial root Tree object. */
    String rootHash;


    /**
     * Construct this new filesystem instance.
     * <P>
     * Used by DarcFileProvider#doCreateFileSystem.
     *
     * @param rootName the URI to the base file as a layered name.
     * @param parentLayer the root file object (tree node)
     * @param fileSystemOptions any additional options (currently none supported)
     * @throws FileSystemException
     */
    public DarcFileSystem(final LayeredFileName rootName,
			final FileObject parentLayer,
			final FileSystemOptions fileSystemOptions)
					throws FileSystemException
	{
		super(rootName, parentLayer, fileSystemOptions);
		FileObject rootFile = parentLayer.getParent().getParent();
		// the filesystem is layered on top of the root tree, blobs are relative to ../.. there
		String refName = rootFile.getName().getRelativeName(rootName.getOuterName());
		rootHash = refName.replaceFirst("/", "");
System.out.println("Setting up BlobStorage at " + rootFile + " and asuming root index at " + refName);
		provider = new BlobStorageProvider(rootFile);
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
		// Release what?
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
	 *
	 * @throws IOException
	 */
	@Override
	protected FileObject createFile(final AbstractFileName name) throws IOException
	{
	    String path = name.getPathDecoded();
//System.out.println("createFile called for " + path);
	    Entry entry = tree.resolveName(path, provider); // throws IOException if not Folder or not Exists
	    // TODO: IMAGINARY?
	    return new DarcFileObject(name, this, entry);
	}

    public BlobStorageProvider getBlobProvider()
    {
        return provider;
    }
}
