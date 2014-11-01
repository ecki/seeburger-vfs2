/*
 * ObjectStorage.java
 *
 * created at 2013-09-18 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DeflaterOutputStream;


/**
 * Tool for reading and writing objects with Git-Header and compression.
 * <P>
 * This object allows to overwrite the method for decorating the streams with
 * compressor ({@link #decorateWithDeflater(OutputStream)}),
 * digester ({@link #decorateWithDigester(OutputStream)}) as well
 * as picking a different digest implementation ({@link #getDigester()}) or the
 * header format ({@link #writeHeader(OutputStream, String, long)}).
 * <P>
 * Instances of this class are reentrant/thread safe.
 *
 * @see #writeBytes(OutputStream, byte[], String)
 */
public class ObjectStorage
{
    private static final Charset ASCII = Charset.forName("ASCII");
    private static final int Kb = 1024;


    public ObjectStorage()
    {
    }


    /**
     * Writes the given bytes to OutputStream in Git format and return hash.
     * <P>
     * This method will close the given <code>out</code> stream.
     *
     * @param out the stream to write the compressed and marked data
     * @param data byte array with full content
     * @param type the magic signature type for the object (see {@link BlobStorageProvider#}
     * @return the hash of the written data (as created by MessageDigester)
     * @throws IOException if any of the IO functions failed or Digest was not found.
     *
     * @see #getDigester()
     * @see #decorateWithDigester(OutputStream)
     */
    byte[] writeBytes(OutputStream out, byte[] data, String type) throws IOException
    {
        OutputStream cout = null;
        DigestOutputStream dout = null;
        try
        {
            cout = decorateWithDeflater(out);
            dout = decorateWithDigester(cout);
            writeHeader(dout, type, data.length);
            dout.write(data, 0, data.length);
            dout.close();
            return dout.getMessageDigest().digest();
        }
        finally
        {
            safeClose(dout, cout, out);
        }
    }

    /**
     * Writes the bytes from the InputStream to OutputStream in Git format and return hash.
     * <P>
     * This method will close the given <code>out</code> and <code>in</code> streams.
     *
     * @return the hash of the written data (as created by MessageDigester)
     * @throws IOException if any of the IO functions failed or Digest was not found.
     *
     * @see #getDigester()
     * @see #decorateWithDigester(OutputStream)
     */
    byte[] writeStream(OutputStream out, InputStream in, long length, String type) throws IOException
    {
        OutputStream cout = null;
        DigestOutputStream dout = null;
        try
        {
            cout = decorateWithDeflater(out);
            dout = decorateWithDigester(cout);
            writeHeader(dout, type, length);
            pumpStreams(in, dout);
            dout.close();
            return dout.getMessageDigest().digest();
        }
        finally
        {
            safeClose(dout, cout, out);
            safeClose(in);
        }
    }


    void pumpStreams(InputStream in, DigestOutputStream dout) throws IOException
    {
        byte[] buf = new byte[32*Kb];
        int n;
        while((n = in.read(buf)) != -1)
        {
            dout.write(buf, 0, n);
        }
    }


    public OutputStream getWriteStream(OutputStream out, String type) throws IOException
    {
        OutputStream cout = null;
        DigestOutputStream dout = null;
        cout = decorateWithDeflater(out);
        dout = decorateWithDigester(cout);
        return /*decorateWithWrapper(*/dout;//);
    }

    /**
     * Very defensive way of closing any of the provided stacked output streams.
     *
     * @param streams variable number of Closeables, the first one not null will be closed
     */
    protected void safeClose(Closeable...streams)
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
                catch (Exception ignored) { /* nothing to recover */ }
            }
        } // for
    }


    protected void writeHeader(OutputStream out, String type, long length) throws IOException
    {
        // ByteBuffer or DataOutputStream would help, but this enjoys compiler/string optimization
        String header = type + " " + String.valueOf(length) + "\0";
        byte[] headerBytes = header.getBytes(ASCII);
        out.write(headerBytes, 0, headerBytes.length);
    }


    DigestOutputStream decorateWithDigester(OutputStream out) throws IOException
    {
        return new DigestOutputStream(out, getDigester());
    }


    protected OutputStream decorateWithDeflater(OutputStream out) throws IOException
    {
        return new DeflaterOutputStream(out);
    }


    MessageDigest getDigester() throws IOException
    {
        try
        {
            return MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException nsa)
        {
            throw new IOException("Installation problem. Unable to create a SHA1 MessageDigest for hashing storage objects.", nsa);
        }
    }
}
