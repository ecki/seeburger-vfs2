package com.seeburger.vfs2.provider.digestarc;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileContentInfo;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.apache.commons.vfs2.util.RandomAccessMode;
import org.apache.commons.vfs2.util.WeakRefFileListener;

import com.seeburger.vfs2.provider.digestarc.DarcFile.File;

public class DarcFileObjectDelegate extends AbstractFileObject implements FileListener
{
	private FileName targetFileName;
	private WeakReference<FileObject> targetFile;
    private boolean ignoreEvent;
    private String hash;

    public DarcFileObjectDelegate(AbstractFileName name,
			AbstractFileSystem fileSystem, File fileDescription)
					throws FileSystemException
	{
        super(name, fileSystem);
		hash = fileDescription.getHash();
		FileObject targetFile = fileSystem.getFileSystemManager().resolveFile("c:\\temp\\object" + "/" + hash);

        this.targetFileName = targetFile.getName();
        if (targetFile != null)
        {
            this.targetFile = new WeakReference<FileObject>(targetFile);
            WeakRefFileListener.installListener(targetFile, this);
        }
	}


    public FileObject getDelegateFile() throws FileSystemException
    {
		WeakReference<FileObject> ref = targetFile;
    	FileObject target = null;
		if (ref != null)
    		target = ref.get();

		if (target != null)
			return target;

		target = getFileSystem().getFileSystemManager().resolveFile(targetFileName.toString());

		targetFile = new WeakReference<FileObject>(target);
		return target;
    }


    /**
     * Attaches or detaches the target file.
     * @param file The FileObject.
     * @throws Exception if an error occurs.
     */
    public void setFile(final FileName target) throws Exception
    {
        final FileType oldType = doGetType();
        targetFileName = target;
        if (target != null)
        {
        	FileObject file = getDelegateFile();
            WeakRefFileListener.installListener(file, this);
        }
        maybeTypeChanged(oldType);
    }

    /**
     * Checks whether the file's type has changed, and fires the appropriate
     * events.
     * @param oldType The old FileType.
     * @throws Exception if an error occurs.
     */
    private void maybeTypeChanged(final FileType oldType) throws Exception
    {
        final FileType newType = doGetType();
        if (oldType == FileType.IMAGINARY && newType != FileType.IMAGINARY)
        {
            handleCreate(newType);
        }
        else if (oldType != FileType.IMAGINARY && newType == FileType.IMAGINARY)
        {
            handleDelete();
        }
    }

    @Override
    protected FileType doGetType() throws FileSystemException
    {
    	return FileType.FILE;
    }

    /**
     * Determines if this file can be read.
     */
    @Override
    protected boolean doIsReadable() throws FileSystemException
    {
    	return true;
    }

    /**
     * Determines if this file can be written to.
     */
    @Override
    protected boolean doIsWriteable() throws FileSystemException
    {
    	return false;
    }

    /**
     * Determines if this file is hidden.
     */
    @Override
    protected boolean doIsHidden() throws FileSystemException
    {
    	FileObject file = getDelegateFile();
        if (file != null)
        {
            return file.isHidden();
        }
        else
        {
            return false;
        }
    }

    /**
     * Lists the children of the file.
     */
    @Override
    protected String[] doListChildren() throws Exception
    {
    	return null;
    }

/*    @Override
    protected void doCreateFolder() throws Exception
    {
        ignoreEvent = true;
        try
        {
            file.createFolder();
        }
        finally
        {
            ignoreEvent = false;
        }
    }*/

/*    @Override
    protected void doDelete() throws Exception
    {
        ignoreEvent = true;
        try
        {
            file.delete();
        }
        finally
        {
            ignoreEvent = false;
        }
    }*/

    @Override
    protected long doGetContentSize() throws Exception
    {
    	// TODO: use cached size
    	FileObject file = getDelegateFile();
    	if (!file.exists())
    		return 17;
        return file.getContent().getSize();
    }

    /**
     * Returns the attributes of this file.
     */
    @Override
    protected Map<String, Object> doGetAttributes()
        throws Exception
    {
    	HashMap<String, Object> ht = new HashMap<String, Object>();
    	ht.put("githash", hash);
    	return ht;
    }

    /**
     * Returns the last-modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime() throws Exception
    {
        return 0; // TODO
    }

    /**
     * Creates an input stream to read the file content from.
     */
    @Override
    protected InputStream doGetInputStream() throws Exception
    {
    	FileObject file = getDelegateFile();
        return file.getContent().getInputStream();
    }

    /**
     * Creates an output stream to write the file content to.
     */
    @Override
    protected OutputStream doGetOutputStream(boolean bAppend) throws Exception
    {
    	FileObject file = getDelegateFile();
        return file.getContent().getOutputStream(bAppend); // TODO
    }

    /**
     * Called when a file is created.
     * @param event The FileChangeEvent.
     * @throws Exception if an error occurs.
     */
    public void fileCreated(final FileChangeEvent event) throws Exception
    {
    	FileObject file = getDelegateFile();
        if (event.getFile() != file)
        {
            return;
        }
        if (!ignoreEvent)
        {
            handleCreate(file.getType());
        }
    }

    /**
     * Called when a file is deleted.
     * @param event The FileChangeEvent.
     * @throws Exception if an error occurs.
     */
    public void fileDeleted(final FileChangeEvent event) throws Exception
    {
    	FileObject file = getDelegateFile();
        if (event.getFile() != file)
        {
            return;
        }
        if (!ignoreEvent)
        {
            handleDelete();
        }
    }

    /**
     * Called when a file is changed.
     * <p/>
     * This will only happen if you monitor the file using {@link org.apache.commons.vfs2.FileMonitor}.
     * @param event The FileChangeEvent.
     * @throws Exception if an error occurs.
     */
    public void fileChanged(FileChangeEvent event) throws Exception
    {
    	FileObject file = getDelegateFile();
        if (event.getFile() != file)
        {
            return;
        }
        if (!ignoreEvent)
        {
            handleChanged();
        }
    }

    /**
     * Close the delegated file.
     * @throws FileSystemException if an error occurs.
     */
    @Override
    public void close() throws FileSystemException
    {
        super.close();

    	FileObject file = getDelegateFile();
        if (file != null)
        {
            file.close();
        }
    }

    /**
     * Refresh file information.
     * @throws FileSystemException if an error occurs.
     * @since 2.0
     */
    @Override
    public void refresh() throws FileSystemException
    {
        super.refresh();
    	FileObject file = getDelegateFile();
        if (file != null)
        {
            file.refresh();
        }
    }

    protected FileContentInfo doGetContentInfo() throws Exception
    {
    	FileObject file = getDelegateFile();
        return file.getContent().getContentInfo();
    }

    @Override
    protected RandomAccessContent doGetRandomAccessContent(final RandomAccessMode mode) throws Exception
    {
    	FileObject file = getDelegateFile();
        return file.getContent().getRandomAccessContent(mode);
    }

}
