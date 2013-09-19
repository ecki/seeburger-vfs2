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
 * Implements the virtual Digest Archive file system which can be
 * layered on top of another VFS2 file-system to provide
 * a Git-like versioned directory.
 * <P>
 * The parent file needs to point to a {@link DarcTree} Object
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
		FileObject rootFile;
        // if this is a folder then this is the Blob base and we dont have a rootHash
		if (parentLayer.getType().hasChildren())
		{
		    rootHash = null;
		    rootFile = parentLayer;
		}
		else if (parentLayer.getType().hasContent()) {
	        rootFile = parentLayer.getParent().getParent();
	        String refName = rootFile.getName().getRelativeName(rootName.getOuterName());
	        rootHash = refName.replaceFirst("/", "");
		}
		else
		{
		    throw new FileSystemException("Cannot produce layered digest archive filesystem, missing root node " + parentLayer);
		}

// System.out.println("Setting up BlobStorage at " + rootFile + " and asuming root index at " + rootHash);

        provider = new BlobStorageProvider(rootFile);
	}


    @Override
	public void init() throws FileSystemException
	{
		super.init();

        InputStream is = null;
        try
        {
            if (rootHash == null)
            {
                tree = new DarcTree();
            }
            else
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
		}
		finally
		{
		    try { if (is != null) is.close(); } catch (Exception ignored) { }
			closeCommunicationLink(); // TODO: why?
		}
	}


	@Override
	protected void doCloseCommunicationLink()
	{
//System.out.println("close link " + this);
	    // Release what?
	}

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
//System.out.println("createFile called for " + name.getPathDecoded());
	    // TODO: IMAGINARY?
	    return new DarcFileObject(name, this, tree);
	}

	/** Used to propagate the current blob provider to freshly created {@link DarcFileObject}s. */
	protected BlobStorageProvider getBlobProvider()
    {
        return provider;
    }

	public String commitChanges() throws IOException
	{
	    return tree.commitChanges(provider);
	}
}
