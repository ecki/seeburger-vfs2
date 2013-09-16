package com.seeburger.vfs2.provider.digestarc;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;

public class DarcTree
{
    static final Charset ASCII = Charset.forName("ASCII");
    static final byte[] BLOB_HEADER = "blob ".getBytes(ASCII);
    static final byte[] EMPTY_BYTES = new byte[0];

    DarcTree.Directory root;

    public DarcTree()
    {
        root = null;
    }

    public DarcTree(Map<String, Entry> entries)
    {
        this.root = new Directory(entries);
    }

    /** Read the root directory of this tree from InputStream. */
    public DarcTree(InputStream is, String expectedHash) throws IOException
    {
        root = new Directory(new DataInputStream(is), expectedHash);
    }


    public Entry resolveName(String name, BlobStorageProvider provider) throws IOException
    {
        if (name.equals("/"))
            return root;

        String[] parts = name.split("/");

        Entry me = root;
        for(int i=1;i<parts.length;i++)
        {
            Entry child = me.getChild(parts[i], provider); // throws if File is reached or child missing
            if (child == null)
            {
                throw new IOException("Cannot resolve " + parts[i] + "(" + i + ") of " + name);
            }
            me = child;
        }

        return me;
    }


    abstract class Entry
    {
        byte[] hash;
        long size;

        abstract Entry getChild(String string, BlobStorageProvider provider) throws IOException;

        abstract String getHash();

        abstract long getSize();

        abstract long getTime();
    }

    class File extends Entry
    {
        long size;
        String hash;

        File(long size, String string)
        {
            this.size = size;
            this.hash = string;
        }

        /**
         * Write length and data to target stream in Git Blob format.
         *
         * @return the hash of the uncompressed data
         * @throws IOException if reading or writing had a problem
         * @throws NoSuchAlgorithmException if the SHA1 MD is unknown
         */
        byte[] writeBlob(OutputStream destination, long size, InputStream source) throws IOException, NoSuchAlgorithmException
        {
            OutputStream zipped = new DeflaterOutputStream(destination);
            DigestOutputStream digester = new DigestOutputStream(zipped, getDigester());

            // the following code ensures the compressor has >32k bytes at the first write, but
            // we also read 32k aligned from source without the need for a BufferedOutputStream
            byte[] buf = new byte[32 * 1025]; // 32k + 32
            System.arraycopy(BLOB_HEADER, 0, buf, 0, 5/*BLOB_HEADER.length*/);

            byte[] lenbytes = String.valueOf(size).getBytes(ASCII);
            System.arraycopy(lenbytes, 0, buf, BLOB_HEADER.length, lenbytes.length);

            int pos = BLOB_HEADER.length + lenbytes.length + 1; // skip one byte, will be '\0'

            int len;
            while((len = source.read(buf, pos, 32*1024)) != -1)
            {
                digester.write(buf, 0, len + pos);
                pos = 0; // next round no offset
            }

            if (pos != 0) // when reading 0 bytes
            {
                digester.write(buf, 0, pos);
            }

            digester.close();
            return digester.getMessageDigest().digest();
        }

        @Override
        String getHash()
        {
            return hash;
        }

        @Override
        Entry getChild(String name, BlobStorageProvider provider) throws IOException
        {
            //throw new FileNotFolderException(name); // TODO: not sure if name arg works as it is relative
            throw new IOException("This Entry is no Folder. name=" + name);
        }

        @Override
        long getSize()
        {
            return size;
        }

        @Override
        long getTime()
        {
            // TODO Auto-generated method stub
            return 0;
        }

    }

    class Directory extends Entry
    {
        private static final byte DIRECTORY_MARKER = 'D';
        private static final byte FILE_MARKER = 'F';

        String hash;
        Map<String, Entry> content;

        Directory(String hash)
        {
            this.hash = hash;
            this.content = null;
        }

        Directory(Map<String, Entry> content)
        {
            this.content = content;
        }

        Entry getChild(String name, BlobStorageProvider provider) throws IOException
        {
            materializeContent(provider);
            return content.get(name); // might be null
        }

        private void materializeContent(BlobStorageProvider provider) throws IOException
        {
            if (content != null)
                return;
            FileObject dir = provider.resolveFileHash(hash);
            try
            {
                FileContent fileContent = dir.getContent();
                InputStream is = fileContent.getInputStream();
                readFromStream(is, hash);
            } finally {
                dir.close();
            }
        }

        /** Read directory from stream and compare the hash.
         * @throws IOException */
        private void readFromStream(InputStream is, String expectedHash) throws IOException
        {
            InflaterInputStream inflated = new InflaterInputStream(is);
            DigestInputStream digester = new DigestInputStream(inflated, getDigester());
            DataInputStream dis = new DataInputStream(digester);
            long len = readHeader(dis, "seetree");
            Map<String, Entry> newContent = new HashMap<String, Entry>(20);
            while(true)
            {
                int type = dis.read();
                if (type == -1)
                    break;
                switch(type)
                {
                    case DIRECTORY_MARKER:
                        byte ver = dis.readByte();
                        if (ver != 1)
                            throw new IOException("Directory Entry with version " + String.valueOf((int)ver)+" is unknown");
                        String name = dis.readUTF();
                        String hash = dis.readUTF();
                        Entry entry = new Directory(hash);
                        newContent.put(name,  entry);
                        break;
                    case FILE_MARKER:
                        ver = dis.readByte();
                        if (ver != 1)
                            throw new IOException("File Entry with version " + String.valueOf((int)ver)+" is unknown");
                        name = dis.readUTF();
                        size = dis.readLong();
                        hash = dis.readUTF();
                        entry = new File(size, hash);
                        newContent.put(name,  entry);
                        break;
                    default:
                        throw new IOException("Unknown record identifier " + String.valueOf((int)type));
                }
            } // end while
            dis.close();
            String digest = asHex(digester.getMessageDigest().digest());
            if (!expectedHash.equals(digest))
                throw new IOException("While readig file with expected hash=" + expectedHash + " we read corrupted data with hash=" + digest);
            content = newContent;
        }

        /** Read and verify the tree header
         * @throws IOException */
        private long readHeader(InputStream in, String header) throws IOException
        {
            int sigLen = header.length() + 1;
            byte[] buf = new byte[sigLen + 19 + 1]; // "tree <long digits>\0"
            in.read(buf, 0, sigLen);
            int i;
            for(i=sigLen;i<buf.length;i++)
            {
                int c = in.read();
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

            String signature = new String(buf, 0, i, ASCII);
            if (!signature.startsWith(header+" "))
            {
                throw new IOException("File Header does not start with signature, but=" + signature);
            }

            return Long.parseLong(signature.substring(sigLen));
        }

        public long getTime()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        Directory(DataInputStream dis, String expectedHash) throws IOException
        {
            readFromStream(dis, expectedHash);
        }

        String writeToStream(OutputStream target) throws IOException
        {
            // First we create a in-memory buffer
            ByteArrayOutputStream bos = new ByteArrayOutputStream(content.size() * 30);
            DataOutputStream out = new DataOutputStream(bos);
            ArrayList<String> names = new ArrayList<String>(content.keySet());
            Collections.sort(names);

            for(String name : names)
            {
                Entry e = content.get(name);
                if (e instanceof Directory)
                {
                    out.writeByte(DIRECTORY_MARKER);
                    out.writeByte(1); // record format version (1=plain)
                    out.writeUTF(name);
                    out.writeUTF(e.getHash());
                }
                else if (e instanceof File)
                {
                    out.writeByte(FILE_MARKER);
                    out.writeByte(1);
                    out.writeUTF(name);
                    out.writeLong(e.getSize());
                    out.writeUTF(e.getHash());
                }
            }
            out.close();
            byte[] buf = bos.toByteArray(); bos = null;

            OutputStream cout = new DeflaterOutputStream(target);
            DigestOutputStream digester = new DigestOutputStream(cout, getDigester());
            // then we write it all to a compressed stream
            DataOutputStream dout = new DataOutputStream(digester);
            dout.writeBytes("seetree ");
            String size = String.valueOf(buf.length);
            dout.writeBytes(size);
            dout.writeByte(0);
            dout.write(buf);
            dout.close();
            return asHex(digester.getMessageDigest().digest());
        }


        public String[] getChildrenNames()
        {
            Set<String> keys = content.keySet();
            return keys.toArray(new String[keys.size()]);
        }

        @Override
        String getHash()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        long getSize()
        {
            // TODO Auto-generated method stub
            return 0;
        }
    }

    public MessageDigest getDigester()
    {
        try
        {
            return MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Cannot resolve SHA1 hash.", e);
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
}