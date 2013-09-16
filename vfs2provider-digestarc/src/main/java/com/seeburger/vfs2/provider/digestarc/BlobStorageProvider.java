/*
 * BlobStorageProvider.java
 *
 * created at 2013-09-14 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved. TODO
 */
package com.seeburger.vfs2.provider.digestarc;


import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;


/**
 * Provides blob loading function to DarcTree.
 * <P>
 * This loader can resolve hashes relative to a base directory.
 * It uses a two-digits sub directory and then the hash name.
 */
public class BlobStorageProvider
{
    private final DarcFileSystem filesystem;
    private final FileName rootName;

    private FileObject rootFile;

    // TODO: use FileObject instead of fs/rootName
    public BlobStorageProvider(DarcFileSystem fs, FileName rootName) throws FileSystemException
    {
        // filesystem manager gets injected to Filesystem after doCreate, so we only store it
        // but not request the manager from it, here.
        this.filesystem = fs;
        this.rootName = rootName;
    }

    /** @return the FileObject matching the specified blob hash. */
    public FileObject resolveFileHash(String hash) throws FileSystemException
    {
        String relPath = hashToPath(hash); // make this specific for each root
        return getBlobRoot().resolveFile(relPath);
    }

    /** @return the FileObject from which blobs can be loaded. */
    private FileObject getBlobRoot() throws FileSystemException
    {
        if (rootFile != null)
        {
            return rootFile;
        }
        rootFile = filesystem.getFileSystemManager().resolveFile(rootName.getURI());
        return rootFile;
    }

    /** Produce a relative path of the given hash (separate first two characters). */
    public String hashToPath(String hash)
    {
        return hash.substring(0, 2) + "/" + hash.substring(2);
    }
}
