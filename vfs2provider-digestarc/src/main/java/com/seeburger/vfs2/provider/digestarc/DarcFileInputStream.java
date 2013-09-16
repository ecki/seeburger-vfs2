/*
 * DarcFileInputStream.java
 *
 * created at 14.09.2013 by Eckenfel <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.StreamHandler;
import java.util.zip.InflaterInputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;


public class DarcFileInputStream extends InflaterInputStream
{
    private static final Charset ASCII = Charset.forName("ASCII");

    private final MessageDigest digester;
    private final String expectedHash;

    private long streamLength;

    public DarcFileInputStream(InputStream in, String expectedHash) throws IOException, NoSuchAlgorithmException
    {
        super(in);

        digester = MessageDigest.getInstance("SHA1");
        this.expectedHash = expectedHash;

        byte[] buf = new byte[25]; // 5 + 19 +1 1
        read(buf, 0, 5); // "blob "
        int i;
        for(i=5;i<buf.length;i++)
        {
            int c = read();
            if (c == -1)
                throw new IOException("EOF while reading header at pos=" + i);
            buf[i] = (byte)c;
            if (c == 0)
                break;
        }
        if (i == buf.length)
        {
            throw new IOException("Missing end of header at pos=" + i);
        }

        String header = new String(buf, 0, i, ASCII);
        if (!header.startsWith("blob "))
        {
            throw new IOException("File Header does not start with signature. hex=" + header);
        }

        this.streamLength = Long.parseLong(header.substring(5));

        System.out.println("stream size is " + streamLength);
    }

    public DarcFileInputStream(FileObject delegatedFile, String hash) throws NoSuchAlgorithmException, FileSystemException, IOException
    {
        this(delegatedFile.getContent().getInputStream(), hash);
    }

    @Override
    public int read(byte[] b, int off, int len)
        throws IOException
    {
        int c = super.read(b, off, len);

        if (c < 0)
            finishDigest();
        else
        {
            System.out.println(" read [" + c);
            digester.update(b, off, c);
        }

        return c;
    }

    // TODO: available
    // TODO: mark/reset


    private void finishDigest() throws IOException
    {
        String streamDigest = asHex(digester.digest());

        if (!streamDigest.equals(expectedHash))
        {
            throw new IOException("Data Corruption, the hashes mismatch. expected=" + expectedHash + " actual=" + streamDigest);
        }
    }

    public static String asHex(byte[] bytes)
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



