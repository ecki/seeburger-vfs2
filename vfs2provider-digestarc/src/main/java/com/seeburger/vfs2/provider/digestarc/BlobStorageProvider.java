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


public class BlobStorageProvider
{
    private final DarcFileSystem filesystem;
    private final FileName rootName;

    private FileObject rootFile;


    public BlobStorageProvider(DarcFileSystem fs, FileName rootName) throws FileSystemException
    {
        // filesystem manager gets injected to Filesystem after doCreate, so we only store it
        this.filesystem = fs;
        this.rootName = rootName;
System.out.println("Loading blobs from " + rootName);
    }


    public FileObject resolveFileHash(String hash) throws FileSystemException
    {
        String relPath = hashToPath(hash); // make this specific for each root
        return getBlobRoot().resolveFile(relPath);
    }


    private FileObject getBlobRoot() throws FileSystemException
    {
        if (rootFile != null)
        {
            return rootFile;
        }
        rootFile = filesystem.getFileSystemManager().resolveFile(rootName.getURI());
        return rootFile;
    }


    /** Use any id string an separate the first 2 characters. */
    public String hashToPath(String hash)
    {
        return hash.substring(0, 2) + "/" + hash.substring(2);
    }
}



