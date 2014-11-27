/*
 * DarcFileProvider.java
 *
 * created at 2013-08-09 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractLayeredFileProvider;
import org.apache.commons.vfs2.provider.FileProvider;
import org.apache.commons.vfs2.provider.LayeredFileName;


/**
 * A file system provider for Digest Archives.
 */
public class DarcFileProvider extends AbstractLayeredFileProvider implements FileProvider
{
    /** The list of capabilities this provider supports */
    protected static final Collection<Capability> capabilities =
        Collections.unmodifiableCollection(Arrays.asList(new Capability[]
        {
            //Capability.GET_LAST_MODIFIED,
            Capability.GET_TYPE,
            Capability.LIST_CHILDREN,
            Capability.READ_CONTENT,
            Capability.URI,
            Capability.COMPRESS,
            Capability.VIRTUAL,

            // TODO: why not work? Capability.APPEND_CONTENT,
            Capability.CREATE,
            Capability.DELETE,
            // Capability.RENAME,
            Capability.WRITE_CONTENT
        }));

    /** The list of capabilities this provider supports */
    protected static final Collection<Capability> writeCapabilities =
        Collections.unmodifiableCollection(Arrays.asList(new Capability[]
        {
             // TOOD Capability.APPEND_CONTENT,
             Capability.CREATE,
             Capability.DELETE,
             // TODO: currently not supported Capability.RENAME,
             Capability.WRITE_CONTENT,
        }));

    public DarcFileProvider()
    {
        super();
    }

    /**
     * Creates a layered file system.  This method is called if the file system
     * is not cached.
     * <P>
     * Logger, Context (and manager) are set afterwards.
     *
     * @param scheme The URI scheme.
     * @param file   The file to create the file system on top of.
     * @return The file system.
     */
    @Override
    protected FileSystem doCreateFileSystem(final String scheme,
                                            final FileObject file,
                                            final FileSystemOptions fileSystemOptions)
        throws FileSystemException
    {
        // not used because findFile() shortcuts it.
        final LayeredFileName rootName =
            new LayeredFileName(scheme, file.getName(), FileName.ROOT_PATH, FileType.FOLDER);
        return new DarcFileSystem(rootName, file, fileSystemOptions);
    }

    @Override
    public FileSystemConfigBuilder getConfigBuilder()
    {
        return DarcFileConfigBuilder.getInstance();
    }

    public Collection<Capability> getCapabilities()
    {
        return capabilities;
    }

    private synchronized FileObject findOrCreateFileSystem(final String scheme,
                                                    final FileName rootName,
                                                    final FileSystemOptions fileSystemOptions)
        throws FileSystemException
    {
        // implementation is based on super.createFileSystem() but
        // without the need for FileObject and using correct cache key.

        // Check if cached
        FileSystem fs = findFileSystem(rootName, fileSystemOptions);
        if (fs == null)
        {
            // Create the file system
            final LayeredFileName name = new LayeredFileName(scheme, rootName, FileName.ROOT_PATH, FileType.FOLDER);
            FileSystemOptions baseOptions = DarcFileConfigBuilder.getInstance().getCleanClone(fileSystemOptions);
            FileObject file = getContext().getFileSystemManager().resolveFile(rootName.getURI(), baseOptions);

            fs = new DarcFileSystem(name, file, fileSystemOptions);

            addFileSystem(rootName, fs);
        }
        return fs.getRoot();
    }


    /**
     * Locates a file object, by absolute URI.
     * <p>
     * This specific implementation makes sure the underlying
     * file system is constructed with a single FS option, so
     * it is read from the cache.
     * <p>
     * It does not use {@link #createFileSystem(String, FileObject, FileSystemOptions)}
     * or {@link #doCreateFileSystem(String, FileObject, FileSystemOptions)}.
     *
     * @param baseFile The base FileObject.
     * @param uri The name of the file to locate.
     * @param properties The FileSystemOptions.
     * @return The FileObject if it is located, null otherwise.
     * @throws FileSystemException if an error occurs.
     */
    public FileObject findFile(final FileObject baseFile,
                               final String uri,
                               final FileSystemOptions properties) throws FileSystemException
    {
        // Split the URI up into its parts
        final LayeredFileName name = (LayeredFileName) parseUri(baseFile != null ? baseFile.getName() : null, uri);

        final FileObject rootFile = findOrCreateFileSystem(name.getScheme(), name.getOuterName(), properties);

        return rootFile.resolveFile(name.getPath());
    }
}
