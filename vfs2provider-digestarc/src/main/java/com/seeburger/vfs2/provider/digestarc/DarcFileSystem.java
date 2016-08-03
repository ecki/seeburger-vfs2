/*
 * DarcFileSystem.java
 *
 * created at 2013-08-09 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.apache.commons.vfs2.provider.LayeredFileName;


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
    /** The provider responsible for looking up hashed names, knows underlying file object. */
    final private BlobStorageProvider provider;
    /** The storage format used to write bytes. */
    final private ObjectStorage storage;

    /** The hash of the initial root Tree object. */
    final String rootHash;
    /** change session identifier, null means read only. */
    final private String changeSession;

    /** In memory structure of this graph. */
    DarcTree tree;


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

        provider = new BlobStorageProvider(rootFile);
        storage = new ObjectStorage();

        DarcFileConfigBuilder config = DarcFileConfigBuilder.getInstance();
        changeSession = config.getChangeSession(fileSystemOptions);
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
            try
            {
                if (is != null)
                {
                    is.close();
                }
            }
            catch (Exception ignored) { /* nothing to recover */ }

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
        if (changeSession == null)
        {
            caps.removeAll(DarcFileProvider.writeCapabilities);
        }
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

    public String commitChanges() throws IOException
    {
        return tree.commitChanges(provider);
    }

    /** Used to propagate the current blob provider to all {@link DarcFileObject}s. */
    protected BlobStorageProvider getBlobProvider()
    {
        return provider;
    }

    /** Used to propagate the current object storage to all {@link DarcFileObject}s. */
    protected ObjectStorage getObjectStorage()
    {
        return storage;
    }
}
