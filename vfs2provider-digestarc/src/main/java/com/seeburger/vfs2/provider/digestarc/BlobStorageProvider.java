/*
 * BlobStorageProvider.java
 *
 * created at 2013-09-14 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved. TODO
 */
package com.seeburger.vfs2.provider.digestarc;


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
    private final FileObject rootFile;

    /** BlobStorageProvider looks up relative hash names. */
    public BlobStorageProvider(FileObject blobRoot) throws FileSystemException
    {
        this.rootFile = blobRoot;
    }

    /** @return the FileObject matching the specified blob hash. */
    public FileObject resolveFileHash(String hash) throws FileSystemException
    {
        String relPath = hashToPath(hash); // make this specific for each root
        return rootFile.resolveFile(relPath);
    }

    /** Produce a relative path of the given hash (separate first two characters). */
    public String hashToPath(String hash)
    {
        return hash.substring(0, 2) + "/" + hash.substring(2);
    }
}
