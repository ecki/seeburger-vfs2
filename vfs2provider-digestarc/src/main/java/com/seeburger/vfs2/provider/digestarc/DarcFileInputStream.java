/*
 * DarcFileInputStream.java
 *
 * created at 2013-09-14 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved. TODO
 */
package com.seeburger.vfs2.provider.digestarc;


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
 * Git blobs use deflater compression and have a "blob <size>\0" header.
 * <P>
 * The various read methods are all delegated by the super class
 * to {@link #read(byte[], int, int)}.
 */
public class DarcFileInputStream extends InflaterInputStream
{
    private static final Charset ASCII = Charset.forName("ASCII");

    private final MessageDigest digester;
    private final String expectedHash;
    private final long streamLength; // TODO: use this information

    /** Wrap a given input stream, will also check the expected hash at the end. */
    public DarcFileInputStream(InputStream in, String expectedHash) throws IOException
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
        this.streamLength = readHeaderLength();
    }

    public DarcFileInputStream(FileObject delegatedFile, String hash) throws FileSystemException, IOException
    {
        this(delegatedFile.getContent().getInputStream(), hash);
    }

    /** Read the Git blob header and return the specified size. */
    private long readHeaderLength() throws IOException
    {
        byte[] buf = new byte[25]; // 5 + 19 +1 1
        read(buf, 0, 5); // "blob "
        int i;
        for(i=5; i < buf.length; i++)
        {
            int c = read();
            if (c == -1)
            {
                throw new IOException("Malformed blob file header. Unexpected end of blob stream at pos=" + i);
            }

            buf[i] = (byte)c;
            if (c == 0)
            {
                break;
            }
        }

        if (i == buf.length)
        {
            throw new IOException("Malformed blob file header, not nul byte till pos=" + i);
        }

        String header = new String(buf, 0, i, ASCII);
        if (!header.startsWith("blob "))
        {
            throw new IOException("Malformed blob file header, does start with unexpected signature=" + header);
        }

        String sizeString = header.substring(5);
        try
        {
            return Long.parseLong(sizeString);
        }
        catch (NumberFormatException ex)
        {
            throw new IOException("Malformed blob file header, cannot parse length argument=" + sizeString, ex);
        }
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

    // TODO: available
    // TODO: mark/reset

    /**
     * Check the digest against the expected value.
     *
     * @throws IOException if the hashes dont match.
     */
    private void finishDigest() throws IOException
    {
        String streamDigest = asHex(digester.digest());

        if (!streamDigest.equals(expectedHash))
        {
            throw new IOException("Data Corruption, the hashes mismatch. expected=" + expectedHash + " actual=" + streamDigest);
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

    // TODO: available/mark/reset
}



