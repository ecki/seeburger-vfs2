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
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.util.WeakRefFileListener;

import com.seeburger.vfs2.provider.digestarc.DarcTree.Directory;
import com.seeburger.vfs2.provider.digestarc.DarcTree.File;


/**
 * Represents Darc File Entries in a virtual tree.
 */
public class DarcFileObject extends AbstractFileObject implements FileListener
{
    public static final String ATTRIBUTE_GITHASH = "githash";

    private final DarcTree tree;
    private final BlobStorageProvider provider;

    private DarcTree.Entry entry;
    private FileType type = FileType.IMAGINARY;
    private WeakReference<FileObject> targetRef;

    private boolean ignoreEvent; // TODO: currently not needed as it is RO
    private ByteArrayOutputStream dataOut;


    protected DarcFileObject(final AbstractFileName name, final DarcFileSystem fs, DarcTree tree)
    		throws FileSystemException
    {
        super(name, fs);
        this.tree = tree;
        this.provider = fs.getBlobProvider();
    }


    /**
     * Determines if this file can be written to.
     *
     * @return {@code true} if this file is writeable, {@code false} if not.
     * @throws FileSystemException if an error occurs.
     */
    @Override
    public boolean isWriteable() throws FileSystemException
    {
        return false; // TODO
    }

    /**
     * Returns the file's type.
     */
    @Override
    protected FileType doGetType()
    {
        return type;
    }

    /**
     * Lists the children of the file.
     * @throws IOException
     */
    @Override
    protected String[] doListChildren() throws IOException
    {
        Directory dir = (Directory)entry;
    	return dir.getChildrenNames(provider);
    }

    @Override
    protected long doGetContentSize()
    {
        File file = (File)entry;
        return file.getSize();
    }

    /**
     * Returns the last modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime() throws Exception
    {
        return entry.getTime();
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
        if (!getType().hasContent())
        {
            throw new FileSystemException("vfs.provider/read-not-file.error", getName());
        }

        FileObject delegatedFile = provider.resolveFileHash(entry.getHash());
        return new DarcFileInputStream(delegatedFile, entry.getHash());
    }

	@Override
	protected void doAttach() throws Exception
	{
	    entry = tree.resolveName(getName().getPathDecoded(), provider);
        if (entry instanceof Directory)
        {
            type = FileType.FOLDER;
        }
        else if (entry instanceof File)
        {
            type = FileType.FILE;
        }

//System.out.println("attached " + getName() + " " + getName().getPath() + " " + type);
	}

	@Override
	protected void doDetach() throws Exception
	{
		type = FileType.IMAGINARY;
//System.out.println("detached " + getName());
	}

    /**
     * Returns the attributes of this file.
     */
    @Override
    protected Map<String, Object> doGetAttributes()
        throws Exception
    {
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
        String hash = entry.getHash();
        FileObject targetFile = provider.resolveFileHash(hash);
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
        tree.delete(getName().getPathDecoded(), provider);
    }

    @Override
    protected void doCreateFolder()
        throws Exception
    {
        if (!getFileSystem().hasCapability(Capability.CREATE))
        {
            throw new FileSystemException("No change session specified, cannot create folder");
        }
        tree.createFolder(getName().getPathDecoded(), provider);
    }

    @Override
    protected void onChange()
        throws Exception
    {
        // super.onChange(); does nothing
// System.out.println("Change received " + getName());
    }

    @Override
    protected void onChildrenChanged(FileName child, FileType newType)
        throws Exception
    {
        // super.onChildrenChanged(child, newType); is empty
// System.out.println("children of " + getName().getPath() + " changed: " + child.getBaseName() + " " + newType);
    }

    @Override
    protected void endOutput()
        throws Exception
    {
        // TODO this is the naive in-memory implementation
        ByteArrayOutputStream  baos = this.dataOut; this.dataOut = null;
        byte[] content = baos.toByteArray(); baos = null;
        long len = content.length;
        OutputStream os = provider.getTempStream();
        ObjectStorage storage = new ObjectStorage();
        byte[] digest = storage.writeBytes(os, content, "blob"); // closes stream
        content = null; // free early
        String hash = provider.storeTempBlob(os, DarcTree.asHex(digest));
        os = null; // free early
        tree.addFile(getName().getPathDecoded(), hash, len, provider);
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
}
