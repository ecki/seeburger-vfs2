/*
 * DarcFileCollectOperation.java
 *
 * created at 2013-10-29 by y.gologanov <y.gologanov@seeburger.com>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;

import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DecoratedFileObject;

import com.seeburger.vfs2.operations.CollectFilesOperation;


/**
 * Collect all delegated files.
 * <p>
 * This class uses the provided collection to add all encountered
 * decorated objects of a Darc subtree. If the collection is not
 * truncated (which is allowed) already collected subdirs will
 * not be reported multiple times.
 */
public class DarcFileCollectOperation implements CollectFilesOperation
{
    private final FileObject root;

    private Collection<String> filesList = null;

    /**
     * @param filesList a Collection where discovered files to be added to.
     */
    public void setFilesList(Collection<String> filesList)
    {
        this.filesList = filesList;
    }

    /**
     * @param root A file tree root object to be iterated
     */
    public DarcFileCollectOperation(FileObject root)
    {
        this.root = root;
    }

    @Override
    public void process()
        throws FileSystemException
    {
        final DarcFileObject darcFile = unwrapDarcFile(root);

        if (darcFile == null)
            return;

        process(darcFile);
    }

    private void process(final DarcFileObject darcFile)
        throws FileSystemException
    {
        // by using the internal getDelefateURI() we
        // avoid the getDelegateFile() API which caches and
        // decorates all files with a FileListener.
        final String uri = darcFile.getDelegateURI();

        // the following only works for Set-like "no change on duplicate" behavior
        final boolean existed = !filesList.add(uri); // returns true if changed

        // if this folder was already visited there is no need
        // to do it again. This will also reduce the chance of loops
        if (existed)
        {
            return;
        }

        if (darcFile.getType().hasChildren())
        {
            FileObject[] fileList = darcFile.getChildren();
            for (FileObject n : fileList)
            {
                // typically no unwrap needed
                DarcFileObject nextFile = unwrapDarcFile(n);
                if (nextFile == null)
                    continue;
                process(nextFile);
            }
        }
    }

    /**
     * Cast or unwrap FileObjects into DarcFileObject.
     *
     * @param a FileObject or null
     * @return the cast file object or null otherwise
     */
    private DarcFileObject unwrapDarcFile(final FileObject file)
    {
        FileObject r = file;
        // && not needed: will secure about DarcFO extending DecoratedFO in future
        while (r instanceof DecoratedFileObject && !(r instanceof DarcFileObject))
        {
            r = ((DecoratedFileObject)r).getDecoratedFileObject();
        }

        if (!(r instanceof DarcFileObject))
        {
            return null;
        }

        return (DarcFileObject)r;
    }
}
