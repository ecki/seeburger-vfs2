/*
 * DarcFileInputStream.java
 *
 * created at 2013-09-16 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.InflaterInputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;


/**
 * Input Stream which allows reading and verifying Git Blobs.
 * <P>
 * Git blobs use deflater compression and have a {@code "blob <size>\0"} header.
 * <P>
 * The various read methods are all delegated by the super class
 * to {@link #read(byte[], int, int)}.
 */
public class DarcFileInputStream extends InflaterInputStream
{
    private static final Charset ASCII = Charset.forName("ASCII");

    private final String expectedHash;
    private final String expectedType;
    private final long streamLength; // TODO: use this information

    private MessageDigest digester;

    /** Wrap a given input stream, will also check the expected hash at the end. */
    public DarcFileInputStream(InputStream in, String expectedHash, String expectedType) throws IOException
    {
        super(in);
        try
        {
            digester = MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IOException("Cannot create InputStream because SHA1 digester cannot be loaded.", e);
        }
        this.expectedHash = expectedHash;
        this.expectedType = expectedType;

        // TODO: lasy on first read(), must be AFTER exepcted* fields are set.
        this.streamLength = readHeaderLength();

    }

    public DarcFileInputStream(FileObject file, String hash) throws FileSystemException, IOException
    {
        this(file.getContent().getInputStream(), hash, "blob");
    }

    public DarcFileInputStream(FileObject file, String hash, String type) throws FileSystemException, IOException
    {
        this(file.getContent().getInputStream(), hash, type);
    }

    public DarcFileInputStream(InputStream stream, String hash) throws FileSystemException, IOException
    {
        this(stream, hash, "blob");
    }

    /**
     * Overwritten read method which does feed all read bytes to the digester.
     *
     * @see #digester
     * @see java.util.zip.InflaterInputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, int off, int len)
        throws IOException
    {
        if (digester == null)
            return -1;

        int c = super.read(b, off, len);

        if (c < 0)
        {
            finishDigest();
        }
        else
        {
            digester.update(b, off, c);
        }

        return c;
    }

    /** Read the Git blob header and return the specified size. */
    private long readHeaderLength() throws IOException
    {
        final String expectedHeader = expectedType + " ";
        final int typeLen = expectedHeader.length();
        byte[] bytes = new byte[typeLen + 19 + 1];  // "blob <long digits>\0"

        try
        {
            DarcFileUtil.readFully(this, bytes, 0, typeLen); // "blob "
        }
        catch (EOFException eof)
        {
            throw new IOException("Malformed file header. Cannot read signature=" + expectedType, eof);
        }

        int i;
        for(i=typeLen; i < bytes.length; i++)
        {
            int c = read();
            if (c == -1)
            {
                throw new IOException("Malformed blob file header. Unexpected end of blob stream at pos=" + i);
            }

            bytes[i] = (byte)c;

            // length string is \0 terminated
            if (c == 0)
            {
                break;
            }
        }

        if (i == bytes.length)
        {
            throw new IOException("Malformed blob file header, no NUL byte till pos=" + i);
        }

        String header = new String(bytes, 0, i, ASCII);
        if (!header.startsWith(expectedHeader))
        {
            throw new IOException("Malformed file header. Expecting=" + expectedType + " found=" + header); // TODO: hex
        }

        String sizeString = header.substring(typeLen);
        try
        {
            return Long.parseLong(sizeString);
        }
        catch (NumberFormatException nfe)
        {
            throw new IOException("Malformed blob file header, cannot parse length argument=" + sizeString, nfe);
        }
    }


    /**
     * Check the digest against the expected value.
     *
     * @throws IOException if the hashes dont match.
     */
    private void finishDigest() throws IOException
    {
        byte[] calculated = digester.digest();
        digester = null;

        if (expectedHash != null)
        {
            String streamDigest = asHex(calculated);
            if (!streamDigest.equals(expectedHash))
            {
                throw new IOException("Data Corruption, the hashes mismatch. expected=" + expectedHash + " actual=" + streamDigest + " stream=" + in);
            }
        }
    }

    /** helper to dormat a byte[] into a lower case hex string. */
    private String asHex(byte[] bytes)
    {
        char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        char[] result = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int i = bytes[j] & 0xFF;
            result[j*2] = hexArray[i >> 4];
            result[j*2 + 1] = hexArray[i & 0xf];
        }
        return new String(result);
    }

    public void readAll() throws IOException
    {
        byte[] workBuffer = new byte[1024];

        while(read(workBuffer) != -1) { /* empty */ }
    }

    // TODO: available/mark/reset
}



