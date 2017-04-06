/*
 * BlobStorageProvider.java
 *
 * created at 2013-09-16 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
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
 * Provides blob storing function to {@link DarcTree}.
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
        String relPath = hashToPath(hash);
        return rootFile.resolveFile(relPath, NameScope.DESCENDENT);
    }

    /** Produce a relative path of the given hash (separate first two characters). */
    public static String hashToPath(String hash)
    {
        return hash.substring(0, 2) + "/" + hash.substring(2);
    }

    /** Get output stream for finalisation by @link {@link #storeTempBlob(OutputStream, String)}. */
    public OutputStream getTempStream() throws FileSystemException
    {
        return new ByteArrayOutputStream();
    }

    /**
     * Make sure content exists in file for given digest.
     * <p>
     * The {@code tempStream} must be obtained from {@link #getTempStream()}.
     * <p>
     * This is used to persist (new) files as well as directory blobs.
     */
    public String storeTempBlob(OutputStream tempStream, String digest) throws IOException
    {
        FileObject target = resolveFileHash(digest);
        target.refresh(); // might be deleted in DB by reorg
        switch(target.getType())
        {
            case FOLDER:
                throw new RuntimeException("Destination blob is an unexpected directory: " + target);
            case FILE:
                return digest;
            case IMAGINARY:
                FileObject parent = target.getParent();
                FileObject tmpFile = createTemp(parent);
                OutputStream os = tmpFile.getContent().getOutputStream(false);
                ((ByteArrayOutputStream)tempStream).writeTo(os);
                os.close();
                tmpFile.close();
                try
                {
                    // this might fail if concurrent rename happens
                    tmpFile.moveTo(target);
                }
                catch (FileSystemException ex)
                {
                    tmpFile.delete();
                    // instead of trying to figure out the reason for
                    // the exception we just check if the file is there
                    // or not (the content must be correct)
                    target.refresh();
                    if (target.getType() != FileType.FILE)
                    {
                        throw ex;
                    }
                }
                return digest;
            default:
                throw new RuntimeException("Corrupted blob type: " + target);
        }
    }

    /** Generate imaginary child with temporary file name. */
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
