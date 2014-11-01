/*
 * DarcFileUtil.java
 *
 * created at 2013-09-25 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DecoratedFileObject;
import org.apache.commons.vfs2.provider.DelegateFileObject;


/** Static utility functions to work with digest archive data and files. */
public class DarcFileUtil
{
    /** No need to instantiate this class, private constructor. */
    private DarcFileUtil() { }

    /**
     * Unwrap and cast FileObject to provider specific implementation.
     * <P>
     * Can deal with decorated file objects.
     * @param file
     * @return
     */
    public static DarcFileObject unwrapDarcFile(FileObject file)
    {
        if (file == null)
        {
            return null;
        }
        FileObject current = file;
        for(int i=0;i<10;i++)
        {
            if (current instanceof DarcFileObject)
            {
                return (DarcFileObject)current;
            }
            if (current instanceof DecoratedFileObject)
            {
                current = ((DecoratedFileObject)current).getDecoratedFileObject();
                continue;
            }
            if (current instanceof DelegateFileObject)
            {
                current = ((DelegateFileObject)current).getDelegateFile();
                continue;
            }
            // if null or unknown
            return null;
        }
        return null;
    }

    /**
     * Extract the hash (blob or tree-id) from a given FileObject.
     *
     * @throws FileSystemException
     */
    public static String getDarcFileHash(FileObject file) throws FileSystemException
    {
        DarcFileObject darcFile = unwrapDarcFile(file);
        if (darcFile != null)
        {
            Object att = darcFile.getContent().getAttribute(DarcFileObject.ATTRIBUTE_GITHASH);
            if (att instanceof String)
            {
                return (String)att;
            }
        }
        // TODO: fallback
        return null;
    }

    /**
     * Commit the changes to the filesystem owning the specified file and return new treeID.
     *
     * @param file any file inside a DarcFileSystem
     * @return the new commitID or null if no changes
     * @throws IOException if the commit operation failed the exception is forwarded
     * @throws IllegalArgumentException if the specified file cannot be casted to a DarcFileSystem
     */
    public static String commitChanges(FileObject file) throws IOException
    {
        DarcFileObject darcFile = unwrapDarcFile(file);
        if (darcFile == null)
        {
            throw new IllegalArgumentException("For commiting the digest archive you need to specify a DarcFileObject: " + file);
        }

        FileSystem fs = darcFile.getFileSystem();
        if (!(fs instanceof DarcFileSystem))
        {
            throw new IllegalArgumentException("The class=" + fs.getClass() + " is no DarcFileSystem for " + file);
        }

        DarcFileSystem dfs = (DarcFileSystem)fs;
        return dfs.commitChanges();
    }

    /**
     * Return the digest of the specified object.
     *
     * @param file and FileObject within a digest archive filesystem
     * @return string if it has the githash attribute or null
     * @throws FileSystemException problems while accessing the file (getContent, getAttribztes) are rethrown
     */
    public static String getObjectID(FileObject file) throws FileSystemException
    {
        return (String)file.getContent().getAttribute(DarcFileObject.ATTRIBUTE_GITHASH);
    }

    /**
     * Calculate and return the githash for the content of the file.
     *
     * @param file the file to checksum
     * @return string represenation of git hash or null
     * @throws FileNotFoundException if file was not found
     * @throws IOException when reading problems occured
     */
    public static String calculateBlobDigest(File file) throws IOException
    {
        ObjectStorage store = new ObjectStorage();
        OutputStream nullOut = new NullOutputStream();
        FileInputStream in = new FileInputStream(file);
        DigestOutputStream dout = null;
        try
        {
            dout = store.decorateWithDigester(nullOut);
            store.writeHeader(dout, "blob", file.length());
            store.pumpStreams(in, dout);
            dout.close();
            return DarcTree.asHex(dout.getMessageDigest().digest());
        }
        finally
        {
            safeClose(in);
            safeClose(dout, nullOut);
        }
    }

    /**
     * Close stacked streams. Will try to close one after the other till first suceeds.
     *
     * @param streams variable number of Closeables, the first one not null will be closed
     */
    protected static void safeClose(Closeable...streams)
    {
        for(int i=0;i<streams.length;i++)
        {
            if (streams[i] != null)
            {
                try
                {
                    streams[i].close();
                    break;
                }
                catch (Exception ex) { /* ignored */ }
            }
        } // for
    }


    /**
     * Read exactly {@code len} bytes.
     *
     * @param is stream to read from
     * @param buf the buffer to write to, must be at least {@code pos+len} long.
     * @param offset the starting offset to write into buffer
     * @param len the number of bytes to read
     * @throws IOException if {@code is.read()} reported a problem.
     * @throws EOFException if stream ends before all bytes are read.
     */
    protected static void readFully(InputStream is , byte[] buf, int offset, int len)
        throws IOException, EOFException
    {
        int read = 0;
        int pos = offset;
        while(read < len)
        {
            int l = is.read(buf, pos, (len-read));
            if (l < 0)
            {
                throw new EOFException("Only read " + read + " of " + len + " bytes.");
            }
            pos += l;
            read += l;
        }
    }


    /** OutputStream which swallows everything. */
    public static class NullOutputStream extends OutputStream
    {
        public NullOutputStream()
        {
        }

        @Override
        public void write(int b)
            throws IOException
        { /* does nothing */ }

        @Override
        public void write(byte[] b)
            throws IOException
        { /* does nothing */ }

        @Override
        public void write(byte[] b, int off, int len)
            throws IOException
        { /* does nothing */ }

        @Override
        public void flush()
            throws IOException
        { /* does nothing */ }

        @Override
        public void close()
            throws IOException
        { /* does nothing */ }
    }
}
