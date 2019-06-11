/*
 * DarcFileObject.java
 *
 * created at 2013-08-09 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.util.WeakRefFileListener;

import com.seeburger.vfs2.provider.digestarc.DarcTree.Directory;
import com.seeburger.vfs2.provider.digestarc.DarcTree.Entry;
import com.seeburger.vfs2.provider.digestarc.DarcTree.File;


/**
 * Represents Darc File Entries in a virtual tree.
 */
public class DarcFileObject extends AbstractFileObject<DarcFileSystem> implements FileListener
{
    public static final String ATTRIBUTE_GITHASH = "githash";

    private final DarcTree tree;

    private DarcTree.Entry cachedEntry;
    private WeakReference<FileObject> targetRef;

    private boolean ignoreEvent; // TODO: currently not needed as it is RO
    private ByteArrayOutputStream dataOut;


    protected DarcFileObject(final AbstractFileName name, final DarcFileSystem fs, DarcTree tree)
    		throws FileSystemException
    {
        super(name, fs);
        this.tree = tree;
    }


    @Override
    protected boolean doIsWriteable()
        throws Exception
    {
        return true;
    }

    @Override
    protected void doRename(FileObject newfile)
        throws Exception
    {
        super.doRename(newfile); // TODO: throws not implemented
    }

    /**
     * Returns the file's type.
     * @throws FileSystemException
     */
    @Override
    protected FileType doGetType() throws FileSystemException
    {
        Entry entry = getEntry();
        if (entry instanceof Directory)
        {
            return FileType.FOLDER;
        }
        else if (entry instanceof File)
        {
            return FileType.FILE;
        }
        return null;
    }

    /**
     * Lists and resolve the children of the file.
     * <p>
     * This works around a VFS limitation in regards to URL encoded characters, see
     * DarcBasicTest#testSpecialInName(). It is used instead of {@link #doListChildren()}.
     *
     * @throws IOException if any problem in the underlying provider happens
     * @see AbstractFileObject#getChildren()
     */
    @Override
    protected FileObject[] doListChildrenResolved() throws IOException
    {
        Entry entry = getEntry();
        if (!(entry instanceof Directory))
        {
            return null;
        }

        final Directory dir = (Directory)entry;
        final String[] names = dir.getChildrenNames(getProvider());
        if (names.length == 0)
        {
            return new FileObject[0];
        }

        final String dirPath = ((AbstractFileName)getName()).getPath();
        final FileObject[] children = new FileObject[names.length];
        final FileSystem fs = getFileSystem();
        for(int i=0; i < names.length; i++)
        {
            children[i] = fs.resolveFile(dirPath + "/" + UriParser.encode(names[i]));
        }
        return children;
    }

    /**
     * Fallback implementation to get children names.
     * <p>
     * {@linkplain AbstractFileObject#getChildren() AFO.getChildren} will
     * prefer {@link #doListChildrenResolved()} instead.
     *
     * @see AbstractFileObject#getChildren()
     */
    @Override
    protected String[] doListChildren() throws IOException
    {
        Entry entry = getEntry();
        if (entry instanceof Directory)
        {
            Directory dir = (Directory)entry;
            return UriParser.encode(dir.getChildrenNames(getProvider()));
        }

        // if we are a file or virtual
        return null;
    }

    @Override
    protected long doGetContentSize() throws FileSystemException
    {
        Entry entry = getEntry();
        File file = (File)entry;
        return file.getSize();
    }

    private synchronized Entry getEntry() throws FileSystemException
    {
        Entry entry = cachedEntry;
        if (entry != null)
            return entry;

        String path;
        try
        {
            path = getName().getPathDecoded();
        }
        catch (FileSystemException fse)
        {
            throw new FileSystemException("Cannot decode path for entry {0}.", fse, getName());
        }

        try
        {
            entry = tree.resolveName(path, getProvider());
            cachedEntry = entry;
            return entry;
        }
        catch (FileSystemException fse)
        {
            throw fse;
        }
        catch (IOException ioe)
        {
            throw new FileSystemException("IOException while getting entry {0}.", ioe, getName());
        }
    }


    /**
     * Returns the last modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime() throws Exception
    {
        return 0L; // Not supported by Darc File System
    }

    /**
     * Creates an input stream to read the file content from.  Is only called
     * if  {@link #doGetType} returns {@link FileType#FILE}.  The input stream
     * returned by this method is guaranteed to be closed before this
     * method is called again.
     */
    @Override
    protected InputStream doGetInputStream() throws Exception
    {
        Entry entry = getEntry();
        if (!getType().hasContent())
        {
            throw new FileSystemException("vfs.provider/read-not-file.error", getName());
        }

        FileObject delegatedFile = getProvider().resolveFileHash(entry.getHash());
        return new DarcFileInputStream(delegatedFile, entry.getHash());
    }

	@Override
	protected void doAttach() throws Exception
	{
	    cachedEntry = null;
	}

	@Override
	protected void doDetach() throws Exception
	{
		cachedEntry = null;
	}

    /**
     * Returns the attributes of this file.
     */
    @Override
    protected Map<String, Object> doGetAttributes()
        throws Exception
    {
        Entry entry = getEntry();
        String hash = entry.getHash();
        HashMap<String, Object> ht = new HashMap<String, Object>();
        if (hash != null)
            ht.put(ATTRIBUTE_GITHASH, hash); // TODO constant
        return ht;
    }

    public FileObject getDelegateFile() throws FileSystemException
    {
        // TODO: make sure the weak reference is still valid or needs to re-resolve (different root)
        WeakReference<FileObject> ref = targetRef;
        FileObject target = null;
        if (ref != null)
            target = ref.get();

        if (target != null)
            return target;

        return resolveHash();
    }

    private FileObject resolveHash() throws FileSystemException
    {
        Entry entry = getEntry();
        String hash = entry.getHash();
        FileObject targetFile = getProvider().resolveFileHash(hash);
        if (targetFile != null)
        {
            this.targetRef = new WeakReference<FileObject>(targetFile);
            WeakRefFileListener.installListener(targetFile, this);
            return targetFile;
        }
        throw new FileSystemException("Expected file blob with hash=" + hash + " cannot be resolved"); // TODO
    }


    @Override
    protected OutputStream doGetOutputStream(boolean bAppend)
        throws Exception
    {
        if (!getFileSystem().hasCapability(Capability.WRITE_CONTENT))
        {
            throw new FileSystemException("No change session specified, cannot modify file");
        }
        dataOut = new ByteArrayOutputStream();
        return dataOut;
    }

    @Override
    protected void doDelete()
        throws Exception
    {
        if (!getFileSystem().hasCapability(Capability.DELETE))
        {
            throw new FileSystemException("No change session specified, cannot delete file");
        }
        tree.delete(getName().getPathDecoded(), getProvider());
    }

    @Override
    protected void doCreateFolder()
        throws Exception
    {
        if (!getFileSystem().hasCapability(Capability.CREATE))
        {
            throw new FileSystemException("No change session specified, cannot create folder");
        }
        tree.createFolder(getName().getPathDecoded(), getProvider());
    }

    @Override
    protected void onChange()
        throws Exception
    {
        cachedEntry = null;
    }

    @Override
    protected void onChildrenChanged(FileName child, FileType newType)
        throws Exception
    {
        cachedEntry = null;
    }

    @Override
    protected void endOutput()
        throws Exception
    {
        // TODO this is the naive in-memory implementation
        byte[] content = this.dataOut.toByteArray();
        this.dataOut = null; // free early
        long len = content.length;

        OutputStream os = getProvider().getTempStream();
        byte[] digest = getObjectStorage().writeBytes(os, content, "blob"); // closes stream
        content = null;

        String hash = getProvider().storeTempBlob(os, DarcTree.asHex(digest));
        os = null;

        tree.addFile(getName().getPathDecoded(), hash, len, getProvider());

        super.endOutput(); // call handleCreate or onChange
    }


    public void fileCreated(FileChangeEvent event)
        throws Exception
    {
        if (!ignoreEvent)
        {
            targetRef.clear(); // force re-resolve
        }
    }


    public void fileDeleted(FileChangeEvent event)
        throws Exception
    {
        if (!ignoreEvent)
        {
            targetRef.clear(); // force re-resolve
        }
    }


    public void fileChanged(FileChangeEvent event)
        throws Exception
    {
        if (!ignoreEvent)
        {
            targetRef.clear(); // force re-resolve
        }
    }

    private BlobStorageProvider getProvider()
    {
        return ((DarcFileSystem)getFileSystem()).getBlobProvider();
    }

    private ObjectStorage getObjectStorage()
    {
        return ((DarcFileSystem)getFileSystem()).getObjectStorage();
    }
}
