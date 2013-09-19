/*
 * BlobStorageProvider.java
 *
 * created at 2013-09-14 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved. TODO
 */
package com.seeburger.vfs2.provider.digestarc;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;


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
    public static String hashToPath(String hash)
    {
        return hash.substring(0, 2) + "/" + hash.substring(2);
    }

    public OutputStream getTempStream() throws FileSystemException
    {
        return new ByteArrayOutputStream();
    }

    public String storeTempBlob(OutputStream tmp, String digest) throws IOException
    {
        ByteArrayOutputStream baos = (ByteArrayOutputStream)tmp;
        byte[] content = baos.toByteArray();

        FileObject target = resolveFileHash(digest);
        switch(target.getType())
        {
            case FOLDER:
                throw new RuntimeException("Destination blob is an unexpected directory: " + target);
            case FILE:
                return digest; // TODO: compare
            case IMAGINARY:
                FileObject parent = target.getParent();
                parent.createFolder();
                FileObject tmpObject = createTemp(parent);
                OutputStream os = tmpObject.getContent().getOutputStream(false);
                os.write(content);
                os.close(); tmpObject.close();
                tmpObject.moveTo(target);
                return digest;
            default:
                throw new RuntimeException("Corrupted blob type: " + target);
        }
    }

    private FileObject createTemp(FileObject parent) throws FileSystemException
    {
        FileObject tmp;
        do
        {
            String tempName = "new-" + System.nanoTime() + ".tmp";
            tmp = parent.resolveFile(tempName, NameScope.CHILD);
        } while (tmp.getType() != FileType.IMAGINARY);

        return tmp;
    }
}
